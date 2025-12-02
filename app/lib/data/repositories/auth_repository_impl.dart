import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/error/app_exception.dart';
import '../datasources/local/auth_local_datasource.dart';
import '../datasources/remote/auth_remote_datasource.dart';
import '../models/auth_model.dart';
import '../models/user_model.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    ref.watch(authRemoteDataSourceProvider),
    ref.watch(authLocalDataSourceProvider),
  );
});

class AuthRepository {
  final AuthRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _localDataSource;

  AuthRepository(this._remoteDataSource, this._localDataSource);

  Future<UserModel> login(String email, String password) async {
    try {
      final response = await _remoteDataSource.login(
        LoginRequest(email: email, password: password),
      );

      await _localDataSource.saveTokens(
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
      );
      await _localDataSource.saveUser(response.user);

      return response.user;
    } on DioException catch (e) {
      throw AppException.fromDioError(e);
    }
  }

  Future<UserModel> register({
    required String email,
    required String password,
    required String brandName,
    String? industry,
  }) async {
    try {
      final response = await _remoteDataSource.register(
        RegisterRequest(
          email: email,
          password: password,
          brandName: brandName,
          industry: industry,
        ),
      );

      await _localDataSource.saveTokens(
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
      );
      await _localDataSource.saveUser(response.user);

      return response.user;
    } on DioException catch (e) {
      throw AppException.fromDioError(e);
    }
  }

  Future<void> logout() async {
    try {
      await _remoteDataSource.logout();
    } catch (_) {
      // Ignore logout errors
    } finally {
      await _localDataSource.clearAll();
    }
  }

  Future<UserModel> getProfile() async {
    try {
      final user = await _remoteDataSource.getProfile();
      await _localDataSource.saveUser(user);
      return user;
    } on DioException catch (e) {
      throw AppException.fromDioError(e);
    }
  }

  Future<UserModel> updateProfile(Map<String, dynamic> data) async {
    try {
      final user = await _remoteDataSource.updateProfile(data);
      await _localDataSource.saveUser(user);
      return user;
    } on DioException catch (e) {
      throw AppException.fromDioError(e);
    }
  }

  Future<bool> isLoggedIn() async {
    return await _localDataSource.hasValidToken();
  }

  Future<UserModel?> getCachedUser() async {
    return await _localDataSource.getUser();
  }

  Future<bool> isOnboardingCompleted() async {
    return await _localDataSource.isOnboardingCompleted();
  }

  Future<void> setOnboardingCompleted() async {
    await _localDataSource.setOnboardingCompleted();
  }
}
