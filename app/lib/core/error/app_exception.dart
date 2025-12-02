import 'package:dio/dio.dart';

class AppException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic data;

  AppException({
    required this.message,
    this.statusCode,
    this.data,
  });

  factory AppException.fromDioError(DioException error) {
    String message;
    int? statusCode;
    dynamic data;

    switch (error.type) {
      case DioExceptionType.connectionTimeout:
        message = '연결 시간이 초과되었습니다.';
        break;
      case DioExceptionType.sendTimeout:
        message = '요청 전송 시간이 초과되었습니다.';
        break;
      case DioExceptionType.receiveTimeout:
        message = '응답 수신 시간이 초과되었습니다.';
        break;
      case DioExceptionType.badCertificate:
        message = '보안 인증서 오류가 발생했습니다.';
        break;
      case DioExceptionType.badResponse:
        statusCode = error.response?.statusCode;
        data = error.response?.data;
        message = _handleStatusCode(statusCode, data);
        break;
      case DioExceptionType.cancel:
        message = '요청이 취소되었습니다.';
        break;
      case DioExceptionType.connectionError:
        message = '인터넷 연결을 확인해주세요.';
        break;
      case DioExceptionType.unknown:
        message = '알 수 없는 오류가 발생했습니다.';
        break;
    }

    return AppException(
      message: message,
      statusCode: statusCode,
      data: data,
    );
  }

  static String _handleStatusCode(int? statusCode, dynamic data) {
    // Try to get message from response data
    if (data is Map<String, dynamic> && data.containsKey('message')) {
      return data['message'];
    }

    switch (statusCode) {
      case 400:
        return '잘못된 요청입니다.';
      case 401:
        return '인증이 필요합니다. 다시 로그인해주세요.';
      case 403:
        return '접근 권한이 없습니다.';
      case 404:
        return '요청하신 정보를 찾을 수 없습니다.';
      case 409:
        return '이미 존재하는 데이터입니다.';
      case 422:
        return '요청 데이터가 올바르지 않습니다.';
      case 429:
        return '너무 많은 요청입니다. 잠시 후 다시 시도해주세요.';
      case 500:
        return '서버 오류가 발생했습니다.';
      case 502:
        return '서버가 일시적으로 응답하지 않습니다.';
      case 503:
        return '서비스를 일시적으로 사용할 수 없습니다.';
      default:
        return '오류가 발생했습니다. (코드: $statusCode)';
    }
  }

  @override
  String toString() => message;
}

class NetworkException extends AppException {
  NetworkException() : super(message: '인터넷 연결을 확인해주세요.');
}

class UnauthorizedException extends AppException {
  UnauthorizedException() : super(message: '인증이 만료되었습니다. 다시 로그인해주세요.', statusCode: 401);
}

class ValidationException extends AppException {
  final Map<String, List<String>>? errors;

  ValidationException({
    required super.message,
    this.errors,
  });
}
