import type { RecordDetail, RecordInput } from "@/types/record";
import { apiFetch } from "./client";

export function registerRecord(input: RecordInput) {
  return apiFetch<RecordDetail>("/api/records", {
    method: "POST",
    body: input,
  });
}

export function searchRecord(id: number) {
  return apiFetch<RecordDetail>(`/api/records/${id}`);
}
