import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../presentation/providers/auth_provider.dart';
import '../../presentation/screens/splash/splash_screen.dart';
import '../../presentation/screens/auth/login_screen.dart';
import '../../presentation/screens/auth/register_screen.dart';
import '../../presentation/screens/dashboard/dashboard_screen.dart';
import '../../presentation/screens/rules/rules_screen.dart';
import '../../presentation/screens/rules/rule_form_screen.dart';
import '../../presentation/screens/channels/channels_screen.dart';
import '../../presentation/screens/logs/message_logs_screen.dart';
import '../../presentation/screens/settings/settings_screen.dart';
import '../../presentation/screens/subscription/subscription_screen.dart';
import '../../presentation/widgets/common/main_scaffold.dart';

final routerProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authStateProvider);

  return GoRouter(
    initialLocation: '/splash',
    debugLogDiagnostics: true,
    redirect: (context, state) {
      final isLoggedIn = authState.valueOrNull?.isLoggedIn ?? false;
      final isLoading = authState.isLoading;
      final isSplash = state.matchedLocation == '/splash';
      final isAuthRoute = state.matchedLocation.startsWith('/auth');

      // Still loading, stay on splash
      if (isLoading && isSplash) return null;

      // If not logged in and not on auth route, redirect to login
      if (!isLoggedIn && !isAuthRoute && !isSplash) {
        return '/auth/login';
      }

      // If logged in and on auth route, redirect to dashboard
      if (isLoggedIn && isAuthRoute) {
        return '/dashboard';
      }

      return null;
    },
    routes: [
      GoRoute(
        path: '/splash',
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: '/auth/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/auth/register',
        builder: (context, state) => const RegisterScreen(),
      ),
      ShellRoute(
        builder: (context, state, child) => MainScaffold(child: child),
        routes: [
          GoRoute(
            path: '/dashboard',
            builder: (context, state) => const DashboardScreen(),
          ),
          GoRoute(
            path: '/rules',
            builder: (context, state) => const RulesScreen(),
            routes: [
              GoRoute(
                path: 'new',
                builder: (context, state) => const RuleFormScreen(),
              ),
              GoRoute(
                path: ':id',
                builder: (context, state) {
                  final id = int.parse(state.pathParameters['id']!);
                  return RuleFormScreen(ruleId: id);
                },
              ),
            ],
          ),
          GoRoute(
            path: '/channels',
            builder: (context, state) => const ChannelsScreen(),
          ),
          GoRoute(
            path: '/logs',
            builder: (context, state) => const MessageLogsScreen(),
          ),
          GoRoute(
            path: '/settings',
            builder: (context, state) => const SettingsScreen(),
          ),
          GoRoute(
            path: '/subscription',
            builder: (context, state) => const SubscriptionScreen(),
          ),
        ],
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      body: Center(
        child: Text('페이지를 찾을 수 없습니다: ${state.error}'),
      ),
    ),
  );
});
