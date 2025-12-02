import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/constants/api_constants.dart';
import '../../models/dashboard_model.dart';

final dashboardRemoteDataSourceProvider = Provider<DashboardRemoteDataSource>((ref) {
  return DashboardRemoteDataSource(ref.watch(dioClientProvider));
});

class DashboardRemoteDataSource {
  final DioClient _dioClient;

  DashboardRemoteDataSource(this._dioClient);

  Future<DashboardStats> getStats() async {
    final response = await _dioClient.get(ApiConstants.dashboardStats);
    return DashboardStats.fromJson(response.data);
  }

  Future<List<ChartData>> getChartData({String period = '7d'}) async {
    final response = await _dioClient.get(
      ApiConstants.dashboardChart,
      queryParameters: {'period': period},
    );
    return (response.data as List)
        .map((json) => ChartData.fromJson(json))
        .toList();
  }

  Future<UsageInfo> getUsageInfo() async {
    final response = await _dioClient.get('${ApiConstants.profile}/usage');
    return UsageInfo.fromJson(response.data);
  }
}
