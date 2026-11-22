import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async headers() {
    return [
      {
        // 同梱した Twemoji の画像（SVG）は、版の番号つきのフォルダで中身を差し替えないので、1 年キャッシュする
        // （版を上げるときは新しいフォルダにし、ここの版の番号と、twemoji-image.tsx の TWEMOJI_PATH もそろえて変える）
        source: "/twemoji/17.0.3/:file([0-9a-f-]+\\.svg)",
        headers: [
          {
            key: "Cache-Control",
            value: "public, max-age=31536000, immutable",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
