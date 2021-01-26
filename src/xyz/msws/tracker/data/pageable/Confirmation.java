package xyz.msws.tracker.data.pageable;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;

public class Confirmation {
	private Client client;
	private String text;

	private Callback<GuildMessageReactionAddEvent> confirm, cancel;

	public Confirmation(Client client, String text) {
		this.client = client;
		this.text = text;

		this.cancel = new Callback<GuildMessageReactionAddEvent>() {

			@Override
			public void execute(GuildMessageReactionAddEvent call) {
				call.retrieveMessage().queue(m -> m.delete().queue());
			}
		};
	}

	public void send(Message message) {
		Pageable<?> pager = new PageableText(client, text);
		pager.addCallback("✅", confirm);
		pager.addCallback("❌", cancel);
	}

	public void confirm(Callback<GuildMessageReactionAddEvent> c) {
		this.confirm = c;
	}

	public void cancel(Callback<GuildMessageReactionAddEvent> c) {
		this.cancel = c;
	}
}
