package ai.openai.vv.controller;

import com.openai.models.audio.AudioResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SimpleAiController {

	private final ChatClient chatClient;
	private final OpenAiChatModel chatModel;
	private final OpenAiImageModel imageClient;
	private final OpenAiAudioTranscriptionModel audioClient;
	private final OpenAiAudioSpeechModel openAiAudioSpeechModel;


	@Value("${spring.ai.openai.api-key}")
	private String openAiKey;


	/* 智能对话 演示
	 * OpenAI：
	 * @see  https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html#_returning_a_chatresponse
	 *
	 * 中转方式有一定几率出错
	 * */
	@GetMapping("/ai/simple")
	public Map<String, String> completion(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message) {
		;
		var value=chatClient.prompt()

				.user(message).call().content();

		return Map.of("generation",value );
	}

	/* 流式响应 演示
	 * OpenAI：
	 * @see  https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html#_streaming_responses
	 *
	 * 中转方式不行就换openai
	 * */
	@GetMapping(value="/ai/stream",produces="text/sse;charset=UTF-8")
	public Flux<String> stream(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message ) {
		;
		return chatClient.prompt()
				.user(message)
				.stream()
				.content();
	}


	/* 文生图 演示
	 * OpenAI： 目前只有 dall-e-3 和 dall-e-2（更笨且支持更小尺寸） 模型可用
	 * @see  https://docs.spring.io/spring-ai/reference/1.0/api/image/openai-image.html#image-options
	 *       https://platform.openai.com/docs/api-reference/images
	 * 中转方式不行就换openai
	 * */
	@GetMapping(value="/ai/img",produces="text/html")
	public String image(@RequestParam(value = "message", defaultValue = "猫") String message ) throws IOException {

		ImageResponse response = imageClient.call(
				new ImagePrompt(message,
						OpenAiImageOptions.builder()
								.quality("hd")
								.n(1)
								.model("dall-e-3")
								// dall-e-2 256
								.height(1024)
								.width(1024).build()));

		String url = response.getResult().getOutput().getUrl();
		System.out.println(url);

		return "<img src='"+url+"'/>";

	}


	/* 语音转文本  演示
	 * OpenAI： 只有whisper-1模型可用
	 * @see  https://docs.spring.io/spring-ai/reference/1.0/api/audio/transcriptions/openai-transcriptions.html
	 * 		 https://platform.openai.com/docs/api-reference/audio/createTranscription
	 * 中转方式不行就换openai
	 * */
	@GetMapping(value="/ai/audio2text")
	public String audio2text() {
		var transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
				.responseFormat(AudioResponseFormat.TEXT)
				.temperature(0f)
				// 默认
				.model("whisper-1")
				.build();

		// flac、mp3、mp4、mpeg、mpga、m4a、ogg、wav 或 webm。
		var audioFile = new ClassPathResource("/hello.mp3");

		AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
		AudioTranscriptionResponse response = audioClient.call(transcriptionRequest);

		//openAiAudioApi.createTranscription()
		return response.getResult().getOutput();
	}


	/* 文转语音  演示
	 * OpenAI： 目前只有 tts-1 模型可用
	 * @see https://docs.spring.io/spring-ai/reference/1.0/api/audio/speech/openai-speech.html
	 * 		https://platform.openai.com/docs/api-reference/audio/createSpeech
	 * 中转方式不行就换openai
	 * */
	@GetMapping(value="/ai/text2audio")
	public String text2audit() {

		OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
				.model("tts-1")
				.voice(OpenAiAudioSpeechOptions.Voice.ALLOY)
				.responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3)
				.speed(1.0)
				.build();

		TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt("Hello, 大家好我是徐庶", speechOptions);
		TextToSpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);

		byte[] body = response.getResult().getOutput();


		// 将byte[]存为 mp3文件
		try {
			writeByteArrayToMp3(body, System.getProperty("user.dir"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return "ok";
	}

	public static void writeByteArrayToMp3(byte[] audioBytes, String outputFilePath) throws IOException {
		// 创建FileOutputStream实例
		FileOutputStream fos = new FileOutputStream(outputFilePath+"/xushu.mp3");

		// 将字节数组写入文件
		fos.write(audioBytes);

		// 关闭文件输出流
		fos.close();
	}


	/*多模态  演示
	* OpenAI： gpt-4-visual-preview 和 gpt-4o 模型提供多模式支持
	* @see https://docs.spring.io/spring-ai/reference/1.0/api/chat/openai-chat.html#_multimodal
	* 中转方式不行就换openai（需要有4的权限）
	* */
	@GetMapping(value="/ai/mutil")
	public String mutilModel(@RequestParam(value = "message", defaultValue = "你从这个图片中看到了什么？")String message) throws IOException {

		var imageData = new ClassPathResource("/test.png");

		var userMessage = UserMessage.builder()
				.text(message) // content
				.media(new Media(MimeTypeUtils.IMAGE_PNG, imageData)) // media
				.build();

		ChatResponse response = chatModel.call(new Prompt(userMessage,
				OpenAiChatOptions.builder()
						.model("kimi-k2.6")
						.build()));

		return  response.getResult().getOutput().getText();
	}



}
