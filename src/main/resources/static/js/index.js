$(function(){
	$("#publishBtn").click(publish);
});

function publish() {

	$("#publishModal").modal("hide");

	// 获取标题和内容
	let title = $("#recipient-name").val();
	let content = $("#message-text").val();
	// 发送异步请求(POST)
	$.ajax({
		url: CONTEXT_PATH + "/post/add",
		type: 'POST',
		data:JSON.stringify( {"title":title,"content":content}),
		success: function(data) {
			// 在提示框中显示返回消息
			$("#hintBody").text(data.message);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 3000);
		},
		dataType: "json",
		contentType: "application/json"
	});
}