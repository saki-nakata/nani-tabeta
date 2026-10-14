"use client";

import { useState } from "react";
import { Label } from "@/components/ui/label";
import type { RegionDetail } from "@/types/master";

type AreaPickerProps = {
  regionDetails: RegionDetail[];
  areaId: number | null;
  onChange: (areaId: number | null) => void;
  hint?: string;
};

const SELECT_STYLE =
  "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-base disabled:opacity-50 md:text-sm";

export function AreaPicker({
  regionDetails,
  areaId,
  onChange,
  hint,
}: AreaPickerProps) {
  // エリアを選ぶ前に選んだ地方（エリアを選んだあとは、エリアから地方を求める）
  const [pickedRegionId, setPickedRegionId] = useState<number | null>(null);

  const regionId =
    areaId === null
      ? pickedRegionId
      : (regionDetails.find(({ areaList }) =>
          areaList.some((area) => area.id === areaId),
        )?.region.id ?? null);
  const areaList =
    regionDetails.find(({ region }) => region.id === regionId)?.areaList ?? [];
  // 地方を選ぶまでは、地方の選び方を案内する。そのあとは、外から渡された案内を出す
  const message =
    regionId === null
      ? "地方を選択すると、都道府県・国を選択できるようになります"
      : hint;

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-baseline gap-2">
        <Label htmlFor="regionId">エリア</Label>
        {message && (
          <span className="text-xs text-muted-foreground">{message}</span>
        )}
      </div>
      <div className="flex gap-2">
        <select
          id="regionId"
          aria-label="地方"
          value={regionId ?? ""}
          onChange={(event) => {
            setPickedRegionId(
              event.target.value === "" ? null : Number(event.target.value),
            );
            onChange(null);
          }}
          className={SELECT_STYLE}
        >
          <option value="">地方</option>
          {regionDetails.map(({ region }) => (
            <option key={region.id} value={region.id}>
              {region.regionName}
            </option>
          ))}
        </select>
        <select
          id="areaId"
          aria-label="都道府県・国"
          value={areaId ?? ""}
          disabled={regionId === null}
          onChange={(event) => {
            // 都道府県を未選択に戻しても地方が消えないよう、今表示している地方を覚えておく
            setPickedRegionId(regionId);
            onChange(
              event.target.value === "" ? null : Number(event.target.value),
            );
          }}
          className={SELECT_STYLE}
        >
          <option value="">都道府県・国</option>
          {areaList.map((area) => (
            <option key={area.id} value={area.id}>
              {area.areaName}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
