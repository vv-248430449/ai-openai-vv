package ai.openai.vv.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 多模态聊天对话框后端接口（新增，基于 SimpleAiController.mutilModel 的思路扩展）。
 *
 * <p>与 mutilModel 的区别：
 * <ul>
 *   <li>支持多轮对话记忆（Spring AI 2.0 的 ChatMemory + MessageChatMemoryAdvisor）</li>
 *   <li>图片改为前端上传（可选），不再写死 classpath 下的 test.png</li>
 *   <li>以 POST multipart 形式接收，便于前端聊天页 FormData 提交</li>
 * </ul>
 *
 * <p>原 SimpleAiController.mutilModel 保持不动。
 */
@RestController
@RequestMapping("/ai/chat")
public class MultimodalChatController {

	private final ChatClient chatClient;

	/** 进程内对话记忆（单例，跨请求共享），保留最近 20 条消息。 */
	private final ChatMemory chatMemory =
			MessageWindowChatMemory.builder().maxMessages(20).build();

	/** 把记忆织入 ChatClient 请求的 advisor。 */
	private final MessageChatMemoryAdvisor memoryAdvisor =
			MessageChatMemoryAdvisor.builder(chatMemory).build();

	public MultimodalChatController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * 多模态聊天接口。
	 *
	 * @param message        用户文字（必填）
	 * @param image          用户图片（可选）
	 * @param conversationId 会话 id（可选，不传则由后端生成并回传，用于多轮记忆串联）
	 * @return 助手回复文本 + 本次使用的 conversationId
	 */
	@PostMapping(value = "/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, String> chat(
			@RequestParam("message") String message,
			@RequestParam(value = "image", required = false) MultipartFile image,
			@RequestParam(value = "conversationId", required = false) String conversationId) {

		// 会话 id：不传则由后端生成（前端拿到后会沿用，实现多轮记忆）。
		// 用单独的最终变量承载，避免 lambda 引用到会被重新赋值的变量。
		final String cid = (conversationId == null || conversationId.isBlank())
				? UUID.randomUUID().toString()
				: conversationId;

		final boolean hasImage = image != null && !image.isEmpty();

		// 图片转 Resource（getBytes 抛 IOException，需在 lambda 外捕获）
		final org.springframework.core.io.Resource imageResource;
		final org.springframework.util.MimeType imageMime;
		if (hasImage) {
			try {
				imageResource = new org.springframework.core.io.ByteArrayResource(image.getBytes()) {
					@Override
					public String getFilename() {
						return image.getOriginalFilename();
					}
				};
			} catch (IOException e) {
				throw new RuntimeException("读取上传图片失败", e);
			}
			imageMime = MimeTypeUtils.parseMimeType(image.getContentType());
		} else {
			imageResource = null;
			imageMime = null;
		}

		// 构造带（可选）图片的用户消息，并带入多轮记忆
		String reply = chatClient.prompt()
				.user(u -> {
					u.text(message);
					if (hasImage) {
						u.media(imageMime, imageResource);
					}
				})
				.options(OpenAiChatOptions.builder().model("kimi-k2.6"))
				.advisors(memoryAdvisor)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
				.call()
				.content();

		return Map.of("reply", reply, "conversationId", cid);
	}
}
