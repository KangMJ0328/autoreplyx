import { useState } from 'react';
import { Filter, ChevronLeft, ChevronRight, Loader2, AlertCircle, Download, MessageSquare } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { useLogs } from '../hooks/useLogs';

export default function Logs() {
  const [channelFilter, setChannelFilter] = useState('all');
  const { logs, pagination, isLoading, error, fetchLogs, exportLogs } = useLogs();

  const filteredLogs = logs.filter((log) => {
    if (channelFilter === 'all') return true;
    return log.channel === channelFilter;
  });

  const handlePageChange = (page: number) => {
    fetchLogs(page);
  };

  const handleExport = async () => {
    await exportLogs('csv');
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('ko-KR');
  };

  if (isLoading && logs.length === 0) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">메시지 로그를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <AlertCircle className="w-8 h-8 text-red-500 mx-auto mb-4" />
          <p className="text-red-600">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-gray-900 mb-2">메시지 로그</h1>
          <p className="text-gray-600">모든 대화 내역을 확인하세요</p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" onClick={handleExport}>
            <Download className="w-4 h-4 mr-2" />
            내보내기
          </Button>
          <Filter className="w-5 h-5 text-gray-500" />
          <Select value={channelFilter} onValueChange={setChannelFilter}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">전체 채널</SelectItem>
              <SelectItem value="Instagram">Instagram</SelectItem>
              <SelectItem value="KakaoTalk">KakaoTalk</SelectItem>
              <SelectItem value="NaverTalk">NaverTalk</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Empty State */}
      {filteredLogs.length === 0 && !isLoading && (
        <Card>
          <CardContent className="p-12 text-center">
            <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-4">
              <MessageSquare className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-gray-900 mb-2">메시지 로그가 없습니다</h3>
            <p className="text-gray-600">아직 처리된 메시지가 없습니다.</p>
          </CardContent>
        </Card>
      )}

      {/* Logs Table */}
      {filteredLogs.length > 0 && (
        <>
          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>발송 시간</TableHead>
                    <TableHead>채널</TableHead>
                    <TableHead>발신자</TableHead>
                    <TableHead>수신 메시지</TableHead>
                    <TableHead>응답 메시지</TableHead>
                    <TableHead>타입</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredLogs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell className="text-gray-600">{formatTimestamp(log.created_at)}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{log.channel}</Badge>
                      </TableCell>
                      <TableCell className="text-gray-900">{log.sender_id}</TableCell>
                      <TableCell className="max-w-xs">
                        <p className="text-gray-900 truncate">{log.received_message}</p>
                      </TableCell>
                      <TableCell className="max-w-xs">
                        <p className="text-gray-900 truncate">{log.response_message || '-'}</p>
                      </TableCell>
                      <TableCell>
                        <Badge
                          className={
                            log.response_type === 'RULE'
                              ? 'bg-blue-50 text-blue-700 border-blue-200'
                              : log.response_type === 'AI'
                              ? 'bg-purple-50 text-purple-700 border-purple-200'
                              : 'bg-gray-50 text-gray-700 border-gray-200'
                          }
                        >
                          {log.response_type === 'RULE' ? '규칙' : log.response_type === 'AI' ? 'AI' : log.response_type}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Pagination */}
          <div className="flex items-center justify-between mt-6">
            <p className="text-gray-600">
              전체 {pagination.total}개 중 {(pagination.current_page - 1) * pagination.per_page + 1}-
              {Math.min(pagination.current_page * pagination.per_page, pagination.total)}개 표시
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(pagination.current_page - 1)}
                disabled={pagination.current_page === 1}
              >
                <ChevronLeft className="w-4 h-4" />
                이전
              </Button>
              <div className="flex items-center gap-1">
                {Array.from({ length: Math.min(pagination.last_page, 5) }, (_, i) => {
                  const page = i + 1;
                  return (
                    <Button
                      key={page}
                      variant={pagination.current_page === page ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handlePageChange(page)}
                      className="w-8"
                    >
                      {page}
                    </Button>
                  );
                })}
                {pagination.last_page > 5 && (
                  <>
                    <span className="px-2 text-gray-500">...</span>
                    <Button
                      variant={pagination.current_page === pagination.last_page ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handlePageChange(pagination.last_page)}
                      className="w-8"
                    >
                      {pagination.last_page}
                    </Button>
                  </>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(pagination.current_page + 1)}
                disabled={pagination.current_page === pagination.last_page}
              >
                다음
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
