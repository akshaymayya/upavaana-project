export interface DiagnosisData {
  plant_name: string;
  disease_name: string;
  symptoms_matched: string;
  solution: string;
  confidence_note: string;
  is_healthy?: boolean;
}
