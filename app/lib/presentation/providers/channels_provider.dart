import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/remote/channel_remote_datasource.dart';
import '../../data/models/channel_model.dart';

final channelsProvider = AsyncNotifierProvider<ChannelsNotifier, List<ChannelModel>>(() {
  return ChannelsNotifier();
});

class ChannelsNotifier extends AsyncNotifier<List<ChannelModel>> {
  @override
  Future<List<ChannelModel>> build() async {
    final dataSource = ref.watch(channelRemoteDataSourceProvider);
    return dataSource.getChannels();
  }

  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      final dataSource = ref.read(channelRemoteDataSourceProvider);
      return dataSource.getChannels();
    });
  }

  Future<String> getInstagramAuthUrl() async {
    final dataSource = ref.read(channelRemoteDataSourceProvider);
    final result = await dataSource.getInstagramAuthUrl();
    return result.authUrl;
  }

  Future<ChannelModel> connectInstagram(String code) async {
    final dataSource = ref.read(channelRemoteDataSourceProvider);
    final channel = await dataSource.connectInstagram(code);

    final currentChannels = state.valueOrNull ?? [];
    final existingIndex = currentChannels.indexWhere((c) => c.platform == 'instagram');

    if (existingIndex >= 0) {
      state = AsyncValue.data(
        currentChannels.map((c) => c.platform == 'instagram' ? channel : c).toList(),
      );
    } else {
      state = AsyncValue.data([...currentChannels, channel]);
    }

    return channel;
  }

  Future<void> toggleChannel(int id, bool isActive) async {
    final dataSource = ref.read(channelRemoteDataSourceProvider);
    final updatedChannel = await dataSource.toggleChannel(id, isActive);

    final currentChannels = state.valueOrNull ?? [];
    state = AsyncValue.data(
      currentChannels.map((c) => c.id == id ? updatedChannel : c).toList(),
    );
  }

  Future<void> disconnectChannel(int id) async {
    final dataSource = ref.read(channelRemoteDataSourceProvider);
    await dataSource.disconnectChannel(id);

    final currentChannels = state.valueOrNull ?? [];
    state = AsyncValue.data(
      currentChannels.where((c) => c.id != id).toList(),
    );
  }
}
