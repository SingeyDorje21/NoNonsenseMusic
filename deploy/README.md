# Hosting the server

How the public server is set up: one small VPS (Ubuntu 24.04, 1 vCPU, 1 GB RAM) running the backend behind a Cloudflare
tunnel, so the machine needs no open port but SSH. The files in this folder are the ones installed on it.

| File | Installed as |
|---|---|
| `nononsense.service` | `/etc/systemd/system/nononsense.service`: the backend, one uvicorn worker on 127.0.0.1:8000 |
| `nn_guard.py` | `/opt/nnm/guard/nn_guard.py`: refuses request bodies over 1 MB before FastAPI reads them |
| `nononsense-tunnel.service` | `/etc/systemd/system/nononsense-tunnel.service`: the public HTTPS address |
| `nononsense-update` | `/usr/local/bin/nononsense-update`: pull, install, restart, wait for /health |
| `nononsense-backup.cron` | `/etc/cron.d/nononsense-backup`: a nightly `pg_dump`, kept 14 days |

## Steps

1. **System.** `apt install curl git ufw unattended-upgrades postgresql-common`, and a user for the server:
   `useradd --system --create-home --home-dir /opt/nnm --shell /bin/bash nnm`.
2. **PostgreSQL 18** from PGDG (`/usr/share/postgresql-common/pgdg/apt.postgresql.org.sh`, then `apt install postgresql-18`).
   The cluster must be UTF-8: on a minimal image the locale is `C`, the cluster comes up as `SQL_ASCII`, and the server
   stops at startup because `schema.sql` holds non-ASCII text. Create it with
   `pg_createcluster 18 main --encoding UTF8 --locale C.UTF-8 --start` (drop the default one first if it is empty).
   Then `sudo -u postgres createuser nnm` and `sudo -u postgres createdb -O nnm music`: the server connects as `nnm`
   over the local socket, with no password.
3. **The code**, as `nnm`: install [uv](https://docs.astral.sh/uv/), clone the repository to `/opt/nnm/app`, and in
   `backend/` run `uv sync --frozen --no-dev --no-install-project`. (`--no-install-project` because `pyproject.toml`
   names a `backend/README.md` that does not exist; the service runs the code from `src/` instead.)
4. **`backend/.env`** (mode 600, owned by `nnm`), from `.env.example`:
   - `TRUSTED_PROXIES=127.0.0.1/32,::1/128`: the tunnel connects from this machine. Without it every user shares one
     rate-limit bucket. Through the tunnel, X-Forwarded-For carries the visitor's IPv4 or IPv6 address.
   - `JIOSAAVN_KEY`: not in this repository; without it JioSaavn copies do not play.
   - `GENIUS_ACCESS_TOKEN` (genius.com/api-clients): genius.com's own API answers 403 to data-centre addresses, so on a
     VPS Genius notes work only through the official API, which needs the token.
5. **Services:** copy `nn_guard.py` and the two units, `systemctl daemon-reload`, `systemctl enable --now nononsense`.
   Check `curl http://127.0.0.1:8000/health`.
6. **Public address:** install `cloudflared` from `pkg.cloudflare.com` and `systemctl enable --now nononsense-tunnel`.
   Its address is in the log: `journalctl -u nononsense-tunnel -o cat | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com'`.
   This is a quick tunnel: free, no account, but the address changes whenever the service restarts. For a fixed address,
   use a named tunnel on a domain in Cloudflare (`cloudflared service install <token>`, public hostname
   `http://localhost:8000`). The apps need HTTPS: the Mac app allows plain HTTP only to this Mac.
7. **Firewall:** `ufw default deny incoming`, `ufw allow 22/tcp`, `ufw enable`. Nothing else listens publicly.
8. **Backups and updates:** install the cron file (and `mkdir -m 700 /var/backups/nononsense`, owned by `nnm`) and
   `nononsense-update`.

## Notes

- **IPv6 that does not route.** If the VPS has an IPv6 address but no working IPv6 route (check with
  `ping -6 2001:4860:4860::8888`), every call to YouTube Music, JioSaavn and LRCLIB waits on IPv6 first: 12 s instead of
  0.4 s. Prefer IPv4 with `echo 'precedence ::ffff:0:0/96  100' >> /etc/gai.conf`.
- **One worker only.** Jams, live notifications and rate limits are kept in the process's memory.
- **SSH:** use keys, and turn password login off (`PasswordAuthentication no`). Some VPS images enable root password
  login in a file under `/etc/ssh/sshd_config.d/`.
