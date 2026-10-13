// Tests for the Android home-screen widget: the state it is sent, and the presses it sends back.
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nononsense/core/accent.dart';
import 'package:nononsense/core/api.dart';
import 'package:nononsense/core/home_screen_widget.dart';
import 'package:nononsense/core/models.dart';

Track song(String name, {String? image}) =>
    Track(Listing(source: 'jiosaavn', id: 'id-$name', title: name, artists: const ['Kai', 'Mo'], duration: 200), const [], image: image);

final song0 = song('Blue');

PlaylistSummary list(String id, {bool album = false, String role = 'owner'}) =>
    PlaylistSummary(id, 'List $id', 3, 600, kind: album ? 'album' : 'playlist', role: role);

void main() {
  group('state', () {
    test('a playing song: its title, artists, cover, liked heart and the play state', () {
      final s = HomeWidgetState.of(signedIn: true, current: song('Blue'), playing: true, liked: true, recent: const [], tiles: const [], coverPath: '/c.png');
      expect(s['signedIn'], isTrue);
      expect(s['title'], 'Blue');
      expect(s['artist'], 'Kai, Mo');
      expect(s['playing'], isTrue);
      expect(s['liked'], isTrue);
      expect(s['cover'], '/c.png');
      expect(s['resume'], isFalse);
    });

    test('nothing loaded: the most recent play is offered to resume', () {
      final s = HomeWidgetState.of(signedIn: true, current: null, playing: false, liked: false, recent: [song('Old'), song('Older')], tiles: const [], coverPath: null);
      expect(s['resume'], isTrue);
      expect(s['title'], 'Old', reason: 'the song that resume plays');
      expect(s['playing'], isFalse);
    });

    test('nothing loaded and nothing played: no song, no resume', () {
      final s = HomeWidgetState.of(signedIn: true, current: null, playing: false, liked: false, recent: const [], tiles: const [], coverPath: null);
      expect(s['resume'], isFalse);
      expect(s['title'], isNull);
    });

    test('signed out: nothing of the account is sent', () {
      final s = HomeWidgetState.of(signedIn: false, current: song('Blue'), playing: true, liked: true, recent: [song('Old')], tiles: [list('a')], coverPath: '/c.png');
      expect(s, {'signedIn': false});
    });

    test('three recent plays and the tiles, in order', () {
      final s = HomeWidgetState.of(signedIn: true, current: song('Now'), playing: true, liked: false,
          recent: [for (final n in ['a', 'b', 'c', 'd']) song(n)], tiles: [list('x'), list('y')], coverPath: null);
      expect((s['recent'] as List).map((r) => (r as Map)['title']), ['a', 'b', 'c']);
      expect((s['recent'] as List).first, {'title': 'a', 'artist': 'Kai, Mo'});
      expect(s['tiles'], [{'id': 'x', 'name': 'List x'}, {'id': 'y', 'name': 'List y'}]);
    });
  });

  group('accent', () {
    test("the playing cover's colour, made readable on each theme, as the app's own accent", () {
      const song = Color(0xFF2E7D32);
      final s = HomeWidgetState.of(signedIn: true, current: song0, playing: true, liked: false, recent: const [], tiles: const [], coverPath: null, song: song);
      final a = s['accent'] as Map;
      expect(a['light'], Accent.readable(song, Brightness.light).toARGB32());
      expect(a['dark'], Accent.readable(song, Brightness.dark).toARGB32());
    });

    test('the text on the accent is black or white, whichever reads, as on Filled buttons', () {
      final a = HomeWidgetState.of(signedIn: true, current: song0, playing: true, liked: false, recent: const [], tiles: const [], coverPath: null,
          song: const Color(0xFF2E7D32))['accent'] as Map;
      expect(a['onLight'], const Color(0xFFFFFFFF).toARGB32(), reason: 'a dark green in the light theme');
      expect(a['onDark'], const Color(0xFF000000).toARGB32(), reason: 'a light green in the dark theme');
    });

    test('a grey cover, or none: no accent is sent, so the widget keeps its default blue', () {
      final s = HomeWidgetState.of(signedIn: true, current: song0, playing: true, liked: false, recent: const [], tiles: const [], coverPath: null, song: null);
      expect(s.containsKey('accent'), isFalse);
    });
  });

  test('tiles: pinned albums first, then the own playlists, at most four; shared ones never', () {
    final playlists = [list('p1'), list('shared', role: 'viewer'), list('a1', album: true), list('p2'), list('a2', album: true), list('p3')];
    expect(HomeWidgetState.tiles(playlists).map((p) => p.id), ['a1', 'a2', 'p1', 'p2']);
  });

  group('actions', () {
    test('each button press is read back', () {
      expect(WidgetAction.parse('toggle'), const WidgetAction('toggle'));
      expect(WidgetAction.parse('next'), const WidgetAction('next'));
      expect(WidgetAction.parse('previous'), const WidgetAction('previous'));
      expect(WidgetAction.parse('like'), const WidgetAction('like'));
      expect(WidgetAction.parse('resume'), const WidgetAction('resume'));
      expect(WidgetAction.parse('recent:2'), const WidgetAction('recent', '2'));
      expect(WidgetAction.parse('tile:abc'), const WidgetAction('tile', 'abc'));
    });

    test('anything else is ignored', () {
      expect(WidgetAction.parse('format-disk'), isNull);
      expect(WidgetAction.parse('recent:x'), isNull);
      expect(WidgetAction.parse('recent:3'), isNull, reason: 'the widget shows three rows: 0 to 2');
      expect(WidgetAction.parse('tile:'), isNull);
    });

    test('a press that started the app arrives as a nononsense://widget link', () {
      expect(WidgetAction.fromUri(Uri.parse('nononsense://widget/next')), const WidgetAction('next'));
      expect(WidgetAction.fromUri(Uri.parse('nononsense://widget/recent/1')), const WidgetAction('recent', '1'));
      expect(WidgetAction.fromUri(Uri.parse('nononsense://widget/tile/abc')), const WidgetAction('tile', 'abc'));
      expect(WidgetAction.fromUri(Uri.parse('nononsense://playlist/abc')), isNull);
      expect(WidgetAction.fromUri(Uri.parse('https://example.com/widget/next')), isNull);
    });
  });
}
