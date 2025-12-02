class DashboardStats {
  final int totalMessages;
  final int todayMessages;
  final int totalResponses;
  final int aiResponses;
  final double responseRate;
  final double avgResponseTime;
  final int activeChannels;
  final int activeRules;

  DashboardStats({
    required this.totalMessages,
    required this.todayMessages,
    required this.totalResponses,
    required this.aiResponses,
    this.responseRate = 0.0,
    this.avgResponseTime = 0.0,
    required this.activeChannels,
    required this.activeRules,
  });

  factory DashboardStats.fromJson(Map<String, dynamic> json) {
    return DashboardStats(
      totalMessages: json['totalMessages'] as int? ?? 0,
      todayMessages: json['todayMessages'] as int? ?? 0,
      totalResponses: json['totalResponses'] as int? ?? 0,
      aiResponses: json['aiResponses'] as int? ?? 0,
      responseRate: (json['responseRate'] as num?)?.toDouble() ?? 0.0,
      avgResponseTime: (json['avgResponseTime'] as num?)?.toDouble() ?? 0.0,
      activeChannels: json['activeChannels'] as int? ?? 0,
      activeRules: json['activeRules'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
    'totalMessages': totalMessages,
    'todayMessages': todayMessages,
    'totalResponses': totalResponses,
    'aiResponses': aiResponses,
    'responseRate': responseRate,
    'avgResponseTime': avgResponseTime,
    'activeChannels': activeChannels,
    'activeRules': activeRules,
  };
}

class ChartData {
  final String label;
  final int messages;
  final int responses;
  final String date;

  ChartData({
    required this.label,
    required this.messages,
    required this.responses,
    required this.date,
  });

  factory ChartData.fromJson(Map<String, dynamic> json) {
    return ChartData(
      label: json['label'] as String,
      messages: json['messages'] as int? ?? 0,
      responses: json['responses'] as int? ?? 0,
      date: json['date'] as String,
    );
  }
}

class UsageInfo {
  final int used;
  final int limit;
  final String plan;
  final String? expiresAt;

  UsageInfo({
    required this.used,
    required this.limit,
    required this.plan,
    this.expiresAt,
  });

  factory UsageInfo.fromJson(Map<String, dynamic> json) {
    return UsageInfo(
      used: json['used'] as int? ?? 0,
      limit: json['limit'] as int? ?? 100,
      plan: json['plan'] as String? ?? 'free',
      expiresAt: json['expiresAt'] as String?,
    );
  }

  double get usagePercentage => limit > 0 ? (used / limit * 100) : 0;
  int get remaining => limit - used;
  bool get isNearLimit => usagePercentage >= 80;
  bool get isAtLimit => used >= limit;
}
