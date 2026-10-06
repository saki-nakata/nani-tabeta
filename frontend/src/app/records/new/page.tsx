import { RecordForm } from "@/components/record-form";

export default function NewRecordPage() {
  return (
    <main className="flex flex-1 flex-col items-center gap-6 px-4 py-8">
      <h1 className="text-2xl font-bold">記録する</h1>
      <RecordForm />
    </main>
  );
}
