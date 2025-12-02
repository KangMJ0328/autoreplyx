import { MessageSquare, Bot, ListChecks, Plus, Link2, Calendar, FileText, TrendingUp, CheckCircle, Loader2, AlertCircle, RefreshCw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { useNavigate } from 'react-router-dom';
import { useDashboard } from '../hooks/useDashboard';
import { useRules } from '../hooks/useRules';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const CHART_COLORS = ['#3B82F6', '#8B5CF6', '#10B981', '#F59E0B', '#EF4444'];

export default function Dashboard() {
  const navigate = useNavigate();
  const {
    stats: dashboardStats,
    chartData,
    channelStats,
    summary,
    isLoading: statsLoading,
    error: statsError,
    refresh
  } = useDashboard();
  const { rules, isLoading: rulesLoading } = useRules();

  const stats = [
    {
      title: '총 메시지',
      value: dashboardStats?.total_messages?.toLocaleString() || '0',
      change: dashboardStats?.message_change || '+0%',
      gradient: 'from-blue-500 to-blue-600',
      icon: MessageSquare,
    },
    {
      title: 'AI 응답',
      value: dashboardStats?.ai_responses?.toLocaleString() || '0',
      change: dashboardStats?.ai_change || '+0%',
      gradient: 'from-purple-500 to-purple-600',
      icon: Bot,
    },
    {
      title: '규칙 응답',
      value: dashboardStats?.rule_responses?.toLocaleString() || '0',
      change: dashboardStats?.rule_change || '+0%',
      gradient: 'from-green-500 to-green-600',
      icon: ListChecks,
    },
  ];

  const activeRules = rules
    .filter(rule => rule.is_active)
    .slice(0, 5)
    .map(rule => ({
      id: rule.id,
      name: rule.name,
      priority: rule.priority,
      channel: rule.channel || 'Instagram',
      active: rule.is_active,
    }));

  const quickActions = [
    {
      title: '새 규칙 만들기',
      description: '자동응답 규칙 추가',
      icon: Plus,
      path: '/rules',
      color: 'text-blue-600',
      bg: 'bg-blue-50',
    },
    {
      title: '채널 연동',
      description: '메시징 채널 연결',
      icon: Link2,
      path: '/channels',
      color: 'text-purple-600',
      bg: 'bg-purple-50',
    },
    {
      title: '예약 관리',
      description: '고객 예약 확인',
      icon: Calendar,
      path: '/reservations',
      color: 'text-green-600',
      bg: 'bg-green-50',
    },
    {
      title: '메시지 로그',
      description: '대화 내역 확인',
      icon: FileText,
      path: '/logs',
      color: 'text-orange-600',
      bg: 'bg-orange-50',
    },
  ];

  if (statsLoading || rulesLoading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (statsError) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <AlertCircle className="w-8 h-8 text-red-500 mx-auto mb-4" />
          <p className="text-red-600">{statsError}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-2">
          <h1 className="text-gray-900">대시보드</h1>
          <div className="flex items-center gap-3">
            <Button variant="outline" size="sm" onClick={refresh}>
              <RefreshCw className="w-4 h-4 mr-2" />
              새로고침
            </Button>
            <div className="flex items-center gap-2 px-3 py-1.5 bg-green-50 rounded-full">
              <CheckCircle className="w-4 h-4 text-green-600" />
              <span className="text-green-600">정상 작동 중</span>
            </div>
          </div>
        </div>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-blue-50 rounded-lg p-4">
            <p className="text-sm text-blue-600 mb-1">연동된 채널</p>
            <p className="text-2xl font-bold text-blue-700">{summary.connected_channels}</p>
          </div>
          <div className="bg-purple-50 rounded-lg p-4">
            <p className="text-sm text-purple-600 mb-1">활성 규칙</p>
            <p className="text-2xl font-bold text-purple-700">{summary.active_rules}</p>
          </div>
          <div className="bg-orange-50 rounded-lg p-4">
            <p className="text-sm text-orange-600 mb-1">대기 예약</p>
            <p className="text-2xl font-bold text-orange-700">{summary.pending_reservations}</p>
          </div>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card key={stat.title}>
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-gray-600 mb-1">{stat.title}</p>
                    <div className="flex items-baseline gap-2">
                      <h2 className="text-gray-900 text-2xl font-bold">{stat.value}</h2>
                      <span className="text-green-600 flex items-center gap-1 text-sm">
                        <TrendingUp className="w-3 h-3" />
                        {stat.change}
                      </span>
                    </div>
                  </div>
                  <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${stat.gradient} flex items-center justify-center`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        {/* Message Trend Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>메시지 트렌드 (최근 7일)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                    <XAxis
                      dataKey="label"
                      tick={{ fontSize: 12, fill: '#6B7280' }}
                      axisLine={{ stroke: '#E5E7EB' }}
                    />
                    <YAxis
                      tick={{ fontSize: 12, fill: '#6B7280' }}
                      axisLine={{ stroke: '#E5E7EB' }}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: 'white',
                        border: '1px solid #E5E7EB',
                        borderRadius: '8px',
                        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)'
                      }}
                      formatter={(value: number) => [`${value}건`, '메시지']}
                    />
                    <Area
                      type="monotone"
                      dataKey="count"
                      stroke="#3B82F6"
                      fill="url(#colorGradient)"
                      strokeWidth={2}
                    />
                    <defs>
                      <linearGradient id="colorGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.3}/>
                        <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex items-center justify-center h-full text-gray-500">
                  <p>데이터가 없습니다</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Channel Distribution */}
        <Card>
          <CardHeader>
            <CardTitle>채널별 메시지</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {channelStats.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={channelStats}
                      cx="50%"
                      cy="50%"
                      innerRadius={40}
                      outerRadius={80}
                      paddingAngle={5}
                      dataKey="count"
                      nameKey="channel"
                    >
                      {channelStats.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value: number, name: string) => [`${value}건`, name]}
                    />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex items-center justify-center h-full text-gray-500">
                  <p>데이터가 없습니다</p>
                </div>
              )}
            </div>
            {channelStats.length > 0 && (
              <div className="mt-4 space-y-2">
                {channelStats.map((item, index) => (
                  <div key={item.channel} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: CHART_COLORS[index % CHART_COLORS.length] }}
                      />
                      <span className="text-gray-600">{item.channel}</span>
                    </div>
                    <span className="font-medium">{item.count}건</span>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Active Rules */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>활성 규칙</CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/rules')}>
                전체 보기
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {activeRules.length > 0 ? (
              <div className="space-y-3">
                {activeRules.map((rule) => (
                  <div
                    key={rule.id}
                    className="flex items-center justify-between p-3 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-2 h-2 rounded-full bg-blue-500" />
                      <div>
                        <p className="text-gray-900">{rule.name}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="text-xs text-gray-500">우선순위 {rule.priority}</span>
                          <Badge variant="secondary">{rule.channel}</Badge>
                        </div>
                      </div>
                    </div>
                    <Badge variant={rule.active ? 'default' : 'secondary'}>
                      {rule.active ? '활성' : '비활성'}
                    </Badge>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500">
                <ListChecks className="w-12 h-12 mx-auto mb-2 opacity-50" />
                <p>활성화된 규칙이 없습니다</p>
                <Button variant="link" onClick={() => navigate('/rules')} className="mt-2">
                  규칙 만들기
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>빠른 작업</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-3">
              {quickActions.map((action) => {
                const Icon = action.icon;
                return (
                  <button
                    key={action.title}
                    onClick={() => navigate(action.path)}
                    className="p-4 rounded-lg border border-gray-200 hover:border-gray-300 transition-all hover:shadow-sm text-left"
                  >
                    <div className={`w-10 h-10 rounded-lg ${action.bg} flex items-center justify-center mb-3`}>
                      <Icon className={`w-5 h-5 ${action.color}`} />
                    </div>
                    <h3 className="text-gray-900 mb-1">{action.title}</h3>
                    <p className="text-xs text-gray-600">{action.description}</p>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
