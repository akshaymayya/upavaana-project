import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { DiagnosisData } from '../types/diagnosis';

export type SessionDiagnosis = {
  id: string;
  imageUri: string;
  diagnosis: DiagnosisData;
  createdAt: number;
};

type DiagnosisSessionValue = {
  entries: SessionDiagnosis[];
  addDiagnosis: (entry: Omit<SessionDiagnosis, 'id'>) => string;
};

const DiagnosisSessionContext = createContext<DiagnosisSessionValue | null>(null);

export function sessionCardTitle(diagnosis: DiagnosisData): string {
  const plant = diagnosis.plant_name?.trim() || 'Unknown plant';
  const issue = diagnosis.disease_name?.trim() || 'Unidentified Issue';
  return `${plant} — ${issue}`;
}

export function DiagnosisSessionProvider({ children }: { children: React.ReactNode }) {
  const [entries, setEntries] = useState<SessionDiagnosis[]>([]);

  const addDiagnosis = useCallback((entry: Omit<SessionDiagnosis, 'id'>): string => {
    const id = `${entry.createdAt}-${Math.random().toString(36).slice(2, 9)}`;
    setEntries((prev) => [{ ...entry, id }, ...prev]);
    return id;
  }, []);

  const value = useMemo(() => ({ entries, addDiagnosis }), [entries, addDiagnosis]);

  return (
    <DiagnosisSessionContext.Provider value={value}>{children}</DiagnosisSessionContext.Provider>
  );
}

export function useDiagnosisSession(): DiagnosisSessionValue {
  const ctx = useContext(DiagnosisSessionContext);
  if (!ctx) {
    throw new Error('useDiagnosisSession must be used within DiagnosisSessionProvider');
  }
  return ctx;
}
