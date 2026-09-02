export interface DiagnosisData {
  plant_name: string;
  disease_name: string;
  symptoms_matched: string | string[];
  solution: string;
  confidence_note: string;
  is_healthy?: boolean;
}

export function symptomsMatchedList(diagnosis: DiagnosisData): string[] {
  const value = diagnosis.symptoms_matched;
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0);
  }
  if (typeof value === 'string' && value.trim()) {
    return [value];
  }
  return [];
}

export function symptomsMatchedDisplay(diagnosis: DiagnosisData): string {
  return symptomsMatchedList(diagnosis).join('\n');
}

