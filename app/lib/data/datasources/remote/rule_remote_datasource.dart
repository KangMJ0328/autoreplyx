import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/constants/api_constants.dart';
import '../../models/rule_model.dart';

final ruleRemoteDataSourceProvider = Provider<RuleRemoteDataSource>((ref) {
  return RuleRemoteDataSource(ref.watch(dioClientProvider));
});

class RuleRemoteDataSource {
  final DioClient _dioClient;

  RuleRemoteDataSource(this._dioClient);

  Future<List<RuleModel>> getRules() async {
    final response = await _dioClient.get(ApiConstants.rules);
    return (response.data as List)
        .map((json) => RuleModel.fromJson(json))
        .toList();
  }

  Future<RuleModel> getRule(int id) async {
    final response = await _dioClient.get('${ApiConstants.rules}/$id');
    return RuleModel.fromJson(response.data);
  }

  Future<RuleModel> createRule(CreateRuleRequest request) async {
    final response = await _dioClient.post(
      ApiConstants.rules,
      data: request.toJson(),
    );
    return RuleModel.fromJson(response.data);
  }

  Future<RuleModel> updateRule(int id, Map<String, dynamic> data) async {
    final response = await _dioClient.put(
      '${ApiConstants.rules}/$id',
      data: data,
    );
    return RuleModel.fromJson(response.data);
  }

  Future<void> deleteRule(int id) async {
    await _dioClient.delete('${ApiConstants.rules}/$id');
  }

  Future<RuleModel> toggleRule(int id, bool isActive) async {
    final response = await _dioClient.patch(
      '${ApiConstants.rules}/$id/toggle',
      data: {'isActive': isActive},
    );
    return RuleModel.fromJson(response.data);
  }
}
