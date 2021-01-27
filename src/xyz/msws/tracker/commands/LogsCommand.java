package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LogsCommand extends AbstractCommand {

	public LogsCommand(Client client, String name) {
		super(client, name);
		setAliases("viewlogs");
		setDescription("Views bot logs");
		setUsage("");
	}

	@Override
	public void execute(Message message, String[] args) {
		List<String> out = new ArrayList<>();

		if (client.getLogs().isEmpty()) {
			message.getChannel().sendMessage("No logs are available").queue();
			return;
		}

		for (int i = Math.max(client.getLogs().size() - 10, 0); i < client.getLogs().size(); i++)
			out.add(client.getLogs().get(i));

		message.getChannel().sendMessage("```\n" + String.join("\n", out) + "```").queue();
		message.getChannel().sendMessage("Uploading logs...").queue();

		Callback<String> result = new Callback<String>() {

			@Override
			public void execute(String call) {
				String response;
				if (!call.startsWith("http")) {
					response = "Unable to upload logs to pastebin, reason:\n`" + call + "`";
				} else {
					response = "Logs can be found at " + call;
				}
				message.getChannel().sendMessage(response).queue();
			}
		};
		pasteUpload("PlayerTracker Logs (Requested by " + message.getAuthor().getAsTag() + ")",
				String.join("\n", client.getLogs()), result);

	}

	private void pasteUpload(String title, String content, Callback<String> call) {
		new Thread(() -> {
			try {
				URL url = new URL("https://pastebin.com/api/api_post.php");

				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
				con.setDoOutput(true);
				con.setRequestMethod("POST");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0");

				Map<String, Object> params = new LinkedHashMap<>();

				params.put("api_dev_key", "sOlnSraxHHjC9MuvXvkL2lCScGkzKGjF");
				params.put("api_option", "paste");
				params.put("api_paste_code", content);
				params.put("api_paste_name", title);
				params.put("api_paste_expire_date", "1W");

				StringBuilder postData = new StringBuilder();
				for (Map.Entry<String, Object> param : params.entrySet()) {
					if (postData.length() != 0)
						postData.append('&');
					postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
					postData.append('=');
					postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
				}
				byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

				con.getOutputStream().write(postDataBytes);
				if (con.getResponseCode() != 200)
					call.execute(con.getResponseCode() + ": " + con.getResponseMessage());
				Reader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));

				StringBuilder result = new StringBuilder();
				for (int c; (c = in.read()) >= 0; )
					result.append((char) c);
				call.execute(result.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}

		}).start();
	}

}
