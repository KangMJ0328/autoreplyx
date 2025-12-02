import { useState } from 'react';
import { Plus, Edit, Trash2, MessageSquare, Loader2, AlertCircle } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '../components/ui/dialog';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Switch } from '../components/ui/switch';
import { useRules } from '../hooks/useRules';
import { AutoRule } from '../lib/api';

interface RuleFormData {
  name: string;
  match_type: 'EXACT' | 'CONTAINS' | 'REGEX';
  keywords: string;
  response_template: string;
  priority: number;
  channel: string;
  cooldown_seconds: number;
  is_active: boolean;
}

export default function Rules() {
  const { rules, isLoading, error, createRule, updateRule, deleteRule, toggleRule } = useRules();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AutoRule | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [formData, setFormData] = useState<RuleFormData>({
    name: '',
    match_type: 'CONTAINS',
    keywords: '',
    response_template: '',
    priority: 1,
    channel: 'Instagram',
    cooldown_seconds: 60,
    is_active: true,
  });

  const resetForm = () => {
    setFormData({
      name: '',
      match_type: 'CONTAINS',
      keywords: '',
      response_template: '',
      priority: rules.length + 1,
      channel: 'Instagram',
      cooldown_seconds: 60,
      is_active: true,
    });
    setEditingRule(null);
  };

  const handleDelete = async (id: number) => {
    if (confirm('이 규칙을 삭제하시겠습니까?')) {
      await deleteRule(id);
    }
  };

  const handleEdit = (rule: AutoRule) => {
    setEditingRule(rule);
    setFormData({
      name: rule.name,
      match_type: rule.match_type,
      keywords: Array.isArray(rule.keywords) ? rule.keywords.join(', ') : rule.keywords,
      response_template: rule.response_template,
      priority: rule.priority,
      channel: rule.channel || 'Instagram',
      cooldown_seconds: rule.cooldown_seconds,
      is_active: rule.is_active,
    });
    setIsDialogOpen(true);
  };

  const handleAddNew = () => {
    resetForm();
    setIsDialogOpen(true);
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const data = {
        ...formData,
        keywords: formData.keywords.split(',').map(k => k.trim()).filter(Boolean),
      };

      if (editingRule) {
        await updateRule(editingRule.id, data);
      } else {
        await createRule(data as any);
      }
      setIsDialogOpen(false);
      resetForm();
    } catch (err) {
      console.error('Failed to save rule:', err);
    } finally {
      setIsSaving(false);
    }
  };

  const handleToggle = async (id: number) => {
    await toggleRule(id);
  };

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">규칙을 불러오는 중...</p>
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
          <h1 className="text-gray-900 mb-2">자동응답 규칙</h1>
          <p className="text-gray-600">키워드 기반 자동응답을 설정하고 관리하세요</p>
        </div>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={handleAddNew} className="bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700">
              <Plus className="w-4 h-4 mr-2" />
              새 규칙 추가
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>{editingRule ? '규칙 수정' : '새 규칙 추가'}</DialogTitle>
              <DialogDescription>자동응답 규칙을 설정하세요</DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="name">규칙 이름</Label>
                <Input
                  id="name"
                  placeholder="규칙 이름을 입력하세요"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="matchType">매칭 타입</Label>
                  <Select
                    value={formData.match_type}
                    onValueChange={(value: 'EXACT' | 'CONTAINS' | 'REGEX') => setFormData({ ...formData, match_type: value })}
                  >
                    <SelectTrigger id="matchType">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="EXACT">정확히 일치</SelectItem>
                      <SelectItem value="CONTAINS">포함</SelectItem>
                      <SelectItem value="REGEX">정규식</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="keyword">키워드 (쉼표로 구분)</Label>
                  <Input
                    id="keyword"
                    placeholder="가격, 얼마"
                    value={formData.keywords}
                    onChange={(e) => setFormData({ ...formData, keywords: e.target.value })}
                  />
                </div>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="response">응답 텍스트</Label>
                <Textarea
                  id="response"
                  placeholder="자동응답 내용을 입력하세요"
                  rows={4}
                  value={formData.response_template}
                  onChange={(e) => setFormData({ ...formData, response_template: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="priority">우선순위</Label>
                  <Input
                    id="priority"
                    type="number"
                    placeholder="1"
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value) || 1 })}
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="channel">채널</Label>
                  <Select
                    value={formData.channel}
                    onValueChange={(value) => setFormData({ ...formData, channel: value })}
                  >
                    <SelectTrigger id="channel">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Instagram">Instagram</SelectItem>
                      <SelectItem value="KakaoTalk">KakaoTalk</SelectItem>
                      <SelectItem value="NaverTalk">NaverTalk</SelectItem>
                      <SelectItem value="ALL">모든 채널</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="cooldown">쿨다운 (초)</Label>
                  <Input
                    id="cooldown"
                    type="number"
                    placeholder="60"
                    value={formData.cooldown_seconds}
                    onChange={(e) => setFormData({ ...formData, cooldown_seconds: parseInt(e.target.value) || 60 })}
                  />
                </div>
              </div>
              <div className="flex items-center justify-between">
                <Label htmlFor="active">활성화</Label>
                <Switch
                  id="active"
                  checked={formData.is_active}
                  onCheckedChange={(checked) => setFormData({ ...formData, is_active: checked })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                취소
              </Button>
              <Button onClick={handleSave} disabled={isSaving}>
                {isSaving ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    저장 중...
                  </>
                ) : (
                  '저장'
                )}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Empty State */}
      {rules.length === 0 && (
        <div className="text-center py-12">
          <MessageSquare className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-gray-900 mb-2">아직 규칙이 없습니다</h3>
          <p className="text-gray-600 mb-4">첫 번째 자동응답 규칙을 추가해보세요</p>
          <Button onClick={handleAddNew} className="bg-gradient-to-r from-blue-500 to-purple-600">
            <Plus className="w-4 h-4 mr-2" />
            새 규칙 추가
          </Button>
        </div>
      )}

      {/* Rules List */}
      <div className="grid gap-4">
        {rules.map((rule) => (
          <Card key={rule.id}>
            <CardContent className="p-6">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
                      <MessageSquare className="w-5 h-5 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="text-gray-900 mb-1">{rule.name}</h3>
                      <div className="flex items-center gap-2">
                        <Badge variant="secondary">우선순위 {rule.priority}</Badge>
                        <Badge variant="outline">{rule.channel || 'ALL'}</Badge>
                        <Badge
                          variant={rule.is_active ? 'default' : 'secondary'}
                          className="cursor-pointer"
                          onClick={() => handleToggle(rule.id)}
                        >
                          {rule.is_active ? '활성' : '비활성'}
                        </Badge>
                      </div>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-gray-500">매칭 타입:</span>
                      <span className="text-gray-900 ml-2">{rule.match_type}</span>
                    </div>
                    <div>
                      <span className="text-gray-500">키워드:</span>
                      <span className="text-gray-900 ml-2">
                        {Array.isArray(rule.keywords) ? rule.keywords.join(', ') : rule.keywords}
                      </span>
                    </div>
                    <div className="col-span-2">
                      <span className="text-gray-500">응답:</span>
                      <p className="text-gray-900 mt-1">{rule.response_template}</p>
                    </div>
                  </div>
                </div>
                <div className="flex gap-2 ml-4">
                  <Button variant="ghost" size="sm" onClick={() => handleEdit(rule)}>
                    <Edit className="w-4 h-4" />
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => handleDelete(rule.id)}>
                    <Trash2 className="w-4 h-4 text-red-600" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
