import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/remote/message_remote_datasource.dart';
import '../../data/models/message_log_model.dart';

// Filter state
class MessageLogFilter {
  final String? status;
  final String? platform;
  final String? startDate;
  final String? endDate;

  const MessageLogFilter({
    this.status,
    this.platform,
    this.startDate,
    this.endDate,
  });

  MessageLogFilter copyWith({
    String? status,
    String? platform,
    String? startDate,
    String? endDate,
  }) {
    return MessageLogFilter(
      status: status ?? this.status,
      platform: platform ?? this.platform,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
    );
  }

  bool get hasFilters =>
      status != null || platform != null || startDate != null || endDate != null;

  MessageLogFilter clear() => const MessageLogFilter();
}

final messageLogFilterProvider = StateProvider<MessageLogFilter>((ref) {
  return const MessageLogFilter();
});

final messageLogsProvider = AsyncNotifierProvider<MessageLogsNotifier, MessageLogPage>(() {
  return MessageLogsNotifier();
});

class MessageLogsNotifier extends AsyncNotifier<MessageLogPage> {
  int _currentPage = 0;
  static const int _pageSize = 20;

  @override
  Future<MessageLogPage> build() async {
    final filter = ref.watch(messageLogFilterProvider);
    return _fetchLogs(0, filter);
  }

  Future<MessageLogPage> _fetchLogs(int page, MessageLogFilter filter) async {
    final dataSource = ref.read(messageRemoteDataSourceProvider);
    return dataSource.getMessageLogs(
      page: page,
      size: _pageSize,
      status: filter.status,
      platform: filter.platform,
      startDate: filter.startDate,
      endDate: filter.endDate,
    );
  }

  Future<void> refresh() async {
    _currentPage = 0;
    state = const AsyncValue.loading();
    final filter = ref.read(messageLogFilterProvider);
    state = await AsyncValue.guard(() => _fetchLogs(0, filter));
  }

  Future<void> loadMore() async {
    final currentState = state.valueOrNull;
    if (currentState == null || currentState.last) return;

    _currentPage++;
    final filter = ref.read(messageLogFilterProvider);

    try {
      final newPage = await _fetchLogs(_currentPage, filter);
      state = AsyncValue.data(
        MessageLogPage(
          content: [...currentState.content, ...newPage.content],
          totalElements: newPage.totalElements,
          totalPages: newPage.totalPages,
          size: newPage.size,
          number: newPage.number,
          first: currentState.first,
          last: newPage.last,
        ),
      );
    } catch (e) {
      _currentPage--;
    }
  }

  Future<void> retryMessage(int id) async {
    final dataSource = ref.read(messageRemoteDataSourceProvider);
    await dataSource.retryMessage(id);
    await refresh();
  }
}

// Single message log provider
final messageLogProvider = FutureProvider.family<MessageLogModel, int>((ref, id) async {
  final dataSource = ref.watch(messageRemoteDataSourceProvider);
  return dataSource.getMessageLog(id);
});

// Message stats provider
final messageStatsProvider = FutureProvider.autoDispose<Map<String, int>>((ref) async {
  final dataSource = ref.watch(messageRemoteDataSourceProvider);
  return dataSource.getMessageStats();
});
