import { useState, useEffect, useCallback } from 'react';
import { rulesAPI, AutoRule } from '../lib/api';

export function useRules() {
  const [rules, setRules] = useState<AutoRule[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRules = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await rulesAPI.list();
      setRules(data);
    } catch (err: any) {
      setError(err.response?.data?.message || '규칙을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRules();
  }, [fetchRules]);

  const createRule = async (data: Omit<AutoRule, 'id' | 'trigger_count' | 'created_at'>) => {
    const newRule = await rulesAPI.create(data);
    setRules((prev) => [...prev, newRule].sort((a, b) => a.priority - b.priority));
    return newRule;
  };

  const updateRule = async (id: number, data: Partial<AutoRule>) => {
    const updatedRule = await rulesAPI.update(id, data);
    setRules((prev) =>
      prev.map((rule) => (rule.id === id ? updatedRule : rule))
    );
    return updatedRule;
  };

  const deleteRule = async (id: number) => {
    await rulesAPI.delete(id);
    setRules((prev) => prev.filter((rule) => rule.id !== id));
  };

  const toggleRule = async (id: number) => {
    const updatedRule = await rulesAPI.toggleActive(id);
    setRules((prev) =>
      prev.map((rule) => (rule.id === id ? updatedRule : rule))
    );
    return updatedRule;
  };

  const testRule = async (message: string, channel: string) => {
    return await rulesAPI.test(message, channel);
  };

  const reorderRules = async (orderedRules: { id: number; priority: number }[]) => {
    const updated = await rulesAPI.reorder(orderedRules);
    setRules(updated);
    return updated;
  };

  return {
    rules,
    isLoading,
    error,
    fetchRules,
    createRule,
    updateRule,
    deleteRule,
    toggleRule,
    testRule,
    reorderRules,
  };
}
