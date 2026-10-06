// 写真を WebP に変えて、長い辺が maxSide 以下になるように縮める
export async function compressToWebp(
  file: File,
  maxSide: number,
): Promise<Blob> {
  const image = await createImageBitmap(file);
  const scale = Math.min(1, maxSide / Math.max(image.width, image.height));
  const width = Math.round(image.width * scale);
  const height = Math.round(image.height * scale);

  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  canvas.getContext("2d")!.drawImage(image, 0, 0, width, height);
  image.close();

  const blob = await new Promise<Blob | null>((resolve) =>
    canvas.toBlob(resolve, "image/webp", 0.8),
  );
  if (!blob || blob.type !== "image/webp") {
    throw new Error("この端末では写真を変換できませんでした。");
  }
  return blob;
}
