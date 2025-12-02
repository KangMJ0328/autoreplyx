import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../constants/app_constants.dart';
import '../constants/api_constants.dart';

class AuthInterceptor extends Interceptor {
  final Ref _ref;
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  AuthInterceptor(this._ref);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    // Skip auth for login and register
    if (options.path.contains('/auth/login') ||
        options.path.contains('/auth/register')) {
      return handler.next(options);
    }

    final token = await _storage.read(key: AppConstants.accessTokenKey);
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode == 401) {
      // Try to refresh token
      final refreshed = await _refreshToken();
      if (refreshed) {
        // Retry the failed request
        final response = await _retry(err.requestOptions);
        return handler.resolve(response);
      }
    }
    handler.next(err);
  }

  Future<bool> _refreshToken() async {
    try {
      final refreshToken = await _storage.read(key: AppConstants.refreshTokenKey);
      if (refreshToken == null) return false;

      final dio = Dio();
      final response = await dio.post(
        '${ApiConstants.baseUrl}${ApiConstants.refreshToken}',
        data: {'refreshToken': refreshToken},
      );

      if (response.statusCode == 200) {
        final newAccessToken = response.data['accessToken'];
        final newRefreshToken = response.data['refreshToken'];

        await _storage.write(key: AppConstants.accessTokenKey, value: newAccessToken);
        if (newRefreshToken != null) {
          await _storage.write(key: AppConstants.refreshTokenKey, value: newRefreshToken);
        }
        return true;
      }
    } catch (e) {
      // Token refresh failed
    }
    return false;
  }

  Future<Response<dynamic>> _retry(RequestOptions requestOptions) async {
    final token = await _storage.read(key: AppConstants.accessTokenKey);

    final options = Options(
      method: requestOptions.method,
      headers: {
        ...requestOptions.headers,
        'Authorization': 'Bearer $token',
      },
    );

    final dio = Dio();
    return dio.request<dynamic>(
      '${ApiConstants.baseUrl}${requestOptions.path}',
      data: requestOptions.data,
      queryParameters: requestOptions.queryParameters,
      options: options,
    );
  }
}
