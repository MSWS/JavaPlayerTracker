package xyz.msws.tracker.data.wrappers;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.data.pageable.Pageable;
import xyz.msws.tracker.data.pageable.PageableText;

public class Confirmation {
	private final Client client;
	private final String text;

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
		pager.bindTo(message.getAuthor());
		pager.send(message.getTextChannel());
	}

	public void confirm(Callback<GuildMessageReactionAddEvent> c) {
		this.confirm = c;
	}

	public void cancel(Callback<GuildMessageReactionAddEvent> c) {
		this.cancel = c;
	}
}
