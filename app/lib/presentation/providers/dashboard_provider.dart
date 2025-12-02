import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/remote/dashboard_remote_datasource.dart';
import '../../data/models/dashboard_model.dart';

final dashboardStatsProvider = FutureProvider.autoDispose<DashboardStats>((ref) async {
  final dataSource = ref.watch(dashboardRemoteDataSourceProvider);
  return dataSource.getStats();
});

final chartDataProvider = FutureProvider.autoDispose.family<List<ChartData>, String>((ref, period) async {
  final dataSource = ref.watch(dashboardRemoteDataSourceProvider);
  return dataSource.getChartData(period: period);
});

final usageInfoProvider = FutureProvider.autoDispose<UsageInfo>((ref) async {
  final dataSource = ref.watch(dashboardRemoteDataSourceProvider);
  return dataSource.getUsageInfo();
});

// Selected period for chart
final selectedPeriodProvider = StateProvider<String>((ref) => '7d');
