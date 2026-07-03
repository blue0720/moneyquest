export function textResult(data) {
  return {
    content: [{ type: "text", text: JSON.stringify(data, null, 2) }],
  };
}

export function errorResult(message) {
  return {
    content: [{ type: "text", text: message }],
    isError: true,
  };
}

export function withErrorHandling(handler) {
  return async (args) => {
    try {
      return await handler(args);
    } catch (err) {
      return errorResult(`エラー: ${err.message}`);
    }
  };
}
