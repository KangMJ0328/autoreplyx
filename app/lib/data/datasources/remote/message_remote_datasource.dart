import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/constants/api_constants.dart';
import '../../models/message_log_model.dart';

final messageRemoteDataSourceProvider = Provider<MessageRemoteDataSource>((ref) {
  return MessageRemoteDataSource(ref.watch(dioClientProvider));
});

class MessageRemoteDataSource {
  final DioClient _dioClient;

  MessageRemoteDataSource(this._dioClient);

  Future<MessageLogPage> getMessageLogs({
    int page = 0,
    int size = 20,
    String? status,
    String? platform,
    String? startDate,
    String? endDate,
  }) async {
    final queryParams = <String, dynamic>{
      'page': page,
      'size': size,
    };

    if (status != null) queryParams['status'] = status;
    if (platform != null) queryParams['platform'] = platform;
    if (startDate != null) queryParams['startDate'] = startDate;
    if (endDate != null) queryParams['endDate'] = endDate;

    final response = await _dioClient.get(
      ApiConstants.messageLogs,
      queryParameters: queryParams,
    );
    return MessageLogPage.fromJson(response.data);
  }

  Future<MessageLogModel> getMessageLog(int id) async {
    final response = await _dioClient.get('${ApiConstants.messageLogs}/$id');
    return MessageLogModel.fromJson(response.data);
  }

  Future<void> retryMessage(int id) async {
    await _dioClient.post('${ApiConstants.messageLogs}/$id/retry');
  }

  Future<Map<String, int>> getMessageStats() async {
    final response = await _dioClient.get('${ApiConstants.messageLogs}/stats');
    return Map<String, int>.from(response.data);
  }
}
