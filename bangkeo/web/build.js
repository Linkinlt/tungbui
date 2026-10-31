// Dịch phần script sang ES5 để chạy trên WebView đời cũ, ghi ra app/src/main/assets/index.html
const fs = require("fs"), path = require("path"), babel = require("@babel/core");
const src = fs.readFileSync(path.join(__dirname, "index.html"), "utf8");
const regen = fs.readFileSync(require.resolve("regenerator-runtime/runtime.js"), "utf8");
let out = src.replace(/<script>([\s\S]*?)<\/script>/g, (m, code) => {
  const r = babel.transformSync(code, { presets: [["@babel/preset-env", { targets: "chrome 37", modules: false }]], sourceType: "script", compact: false });
  return "<script>" + r.code + "</script>";
});
out = out.replace("<script>", "<script>" + regen + "</script>\n<script>");
fs.writeFileSync(path.join(__dirname, "..", "app", "src", "main", "assets", "index.html"), out);
console.log("ok", out.length);
