import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/constants/api_constants.dart';
import '../../models/channel_model.dart';

final channelRemoteDataSourceProvider = Provider<ChannelRemoteDataSource>((ref) {
  return ChannelRemoteDataSource(ref.watch(dioClientProvider));
});

class ChannelRemoteDataSource {
  final DioClient _dioClient;

  ChannelRemoteDataSource(this._dioClient);

  Future<List<ChannelModel>> getChannels() async {
    final response = await _dioClient.get(ApiConstants.channels);
    return (response.data as List)
        .map((json) => ChannelModel.fromJson(json))
        .toList();
  }

  Future<ChannelModel> getChannel(int id) async {
    final response = await _dioClient.get('${ApiConstants.channels}/$id');
    return ChannelModel.fromJson(response.data);
  }

  Future<InstagramAuthUrl> getInstagramAuthUrl() async {
    final response = await _dioClient.get(ApiConstants.instagramAuth);
    return InstagramAuthUrl.fromJson(response.data);
  }

  Future<ChannelModel> connectInstagram(String code) async {
    final response = await _dioClient.post(
      ApiConstants.instagramCallback,
      data: {'code': code},
    );
    return ChannelModel.fromJson(response.data);
  }

  Future<ChannelModel> toggleChannel(int id, bool isActive) async {
    final response = await _dioClient.patch(
      '${ApiConstants.channels}/$id/toggle',
      data: {'isActive': isActive},
    );
    return ChannelModel.fromJson(response.data);
  }

  Future<void> disconnectChannel(int id) async {
    await _dioClient.delete('${ApiConstants.channels}/$id');
  }

  Future<ChannelModel> refreshChannelToken(int id) async {
    final response = await _dioClient.post('${ApiConstants.channels}/$id/refresh');
    return ChannelModel.fromJson(response.data);
  }
}
