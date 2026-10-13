import 'dart:ui';

import 'package:flutter/material.dart';

/// A restrained backdrop for phone screens. The accent follows the playing cover.
class PhoneBackdrop extends StatelessWidget {
  const PhoneBackdrop({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = scheme.brightness == Brightness.dark;
    return IgnorePointer(
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: dark
                ? [
                    const Color(0xFF10131C),
                    scheme.primary.withValues(alpha: 0.16),
                    const Color(0xFF10131C),
                  ]
                : [
                    const Color(0xFFF8FAFF),
                    scheme.primary.withValues(alpha: 0.12),
                    const Color(0xFFF0F3FA),
                  ],
            stops: const [0, 0.48, 1],
          ),
        ),
      ),
    );
  }
}

/// Frosted surface for the few floating phone elements that benefit from blur.
class GlassSurface extends StatelessWidget {
  const GlassSurface({
    super.key,
    required this.child,
    this.radius = 24,
    this.padding = EdgeInsets.zero,
    this.blur = true,
  });

  final Widget child;
  final double radius;
  final EdgeInsetsGeometry padding;
  final bool blur;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = scheme.brightness == Brightness.dark;
    final shape = BorderRadius.circular(radius);
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: shape,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: dark ? 0.24 : 0.09),
            blurRadius: 26,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: shape,
        child: BackdropFilter(
          filter: ImageFilter.blur(
            sigmaX: blur ? 18 : 0,
            sigmaY: blur ? 18 : 0,
          ),
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: shape,
              border: Border.all(
                color: Colors.white.withValues(alpha: dark ? 0.14 : 0.78),
              ),
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: dark
                    ? [
                        const Color(0xFF343B50).withValues(alpha: 0.76),
                        const Color(0xFF1B2233).withValues(alpha: 0.72),
                      ]
                    : [
                        Colors.white.withValues(alpha: 0.82),
                        const Color(0xFFE8EEFA).withValues(alpha: 0.72),
                      ],
              ),
            ),
            child: Padding(padding: padding, child: child),
          ),
        ),
      ),
    );
  }
}

/// Phone-only slide and fade used by overlays and tab content.
Widget phoneRevealTransition(
  Widget child,
  Animation<double> animation, {
  double dy = 0.06,
}) {
  final curved = CurvedAnimation(
    parent: animation,
    curve: Curves.easeOutCubic,
    reverseCurve: Curves.easeInCubic,
  );
  return FadeTransition(
    opacity: curved,
    child: SlideTransition(
      position: Tween<Offset>(
        begin: Offset(0, dy),
        end: Offset.zero,
      ).animate(curved),
      child: child,
    ),
  );
}
