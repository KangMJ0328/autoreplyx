import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/user_model.dart';
import '../../data/repositories/auth_repository_impl.dart';

// Auth state class
class AuthState {
  final UserModel? user;
  final bool isLoggedIn;
  final bool isLoading;
  final String? error;

  const AuthState({
    this.user,
    this.isLoggedIn = false,
    this.isLoading = false,
    this.error,
  });

  AuthState copyWith({
    UserModel? user,
    bool? isLoggedIn,
    bool? isLoading,
    String? error,
  }) {
    return AuthState(
      user: user ?? this.user,
      isLoggedIn: isLoggedIn ?? this.isLoggedIn,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

// Auth state provider
final authStateProvider = AsyncNotifierProvider<AuthNotifier, AuthState>(() {
  return AuthNotifier();
});

class AuthNotifier extends AsyncNotifier<AuthState> {
  @override
  Future<AuthState> build() async {
    final repository = ref.watch(authRepositoryProvider);

    final isLoggedIn = await repository.isLoggedIn();
    if (isLoggedIn) {
      try {
        final user = await repository.getProfile();
        return AuthState(user: user, isLoggedIn: true);
      } catch (e) {
        // Token might be invalid, clear and return logged out state
        await repository.logout();
        return const AuthState(isLoggedIn: false);
      }
    }

    return const AuthState(isLoggedIn: false);
  }

  Future<void> login(String email, String password) async {
    state = const AsyncValue.loading();

    try {
      final repository = ref.read(authRepositoryProvider);
      final user = await repository.login(email, password);
      state = AsyncValue.data(AuthState(user: user, isLoggedIn: true));
    } catch (e) {
      state = AsyncValue.data(AuthState(
        isLoggedIn: false,
        error: e.toString(),
      ));
      rethrow;
    }
  }

  Future<void> register({
    required String email,
    required String password,
    required String brandName,
    String? industry,
  }) async {
    state = const AsyncValue.loading();

    try {
      final repository = ref.read(authRepositoryProvider);
      final user = await repository.register(
        email: email,
        password: password,
        brandName: brandName,
        industry: industry,
      );
      state = AsyncValue.data(AuthState(user: user, isLoggedIn: true));
    } catch (e) {
      state = AsyncValue.data(AuthState(
        isLoggedIn: false,
        error: e.toString(),
      ));
      rethrow;
    }
  }

  Future<void> logout() async {
    final repository = ref.read(authRepositoryProvider);
    await repository.logout();
    state = const AsyncValue.data(AuthState(isLoggedIn: false));
  }

  Future<void> refreshProfile() async {
    try {
      final repository = ref.read(authRepositoryProvider);
      final user = await repository.getProfile();
      state = AsyncValue.data(AuthState(user: user, isLoggedIn: true));
    } catch (e) {
      // Keep current state on refresh failure
    }
  }

  Future<void> updateProfile(Map<String, dynamic> data) async {
    try {
      final repository = ref.read(authRepositoryProvider);
      final user = await repository.updateProfile(data);
      state = AsyncValue.data(AuthState(user: user, isLoggedIn: true));
    } catch (e) {
      rethrow;
    }
  }
}

// Current user provider (convenient accessor)
final currentUserProvider = Provider<UserModel?>((ref) {
  return ref.watch(authStateProvider).valueOrNull?.user;
});
