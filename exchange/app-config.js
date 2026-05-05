// 文件说明：Web 端调试配置脚本，负责集中声明接口地址与运行参数。

window.__APP_CONFIG__ = Object.assign(
  {
    apiBaseUrl: "",
    previewMode: false,
  },
  window.__APP_CONFIG__ || {},
);
