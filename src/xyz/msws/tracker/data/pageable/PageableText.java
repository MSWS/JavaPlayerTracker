package xyz.msws.tracker.data.pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import xyz.msws.tracker.Client;

public class PageableText extends Pageable<Message> {

	private PageableText(Client client) {
		super(client);
	}

	public PageableText(Client client, Collection<Message> pages) {
		this(client);
		this.pages = new ArrayList<>(pages);
	}

	public PageableText(Client client, Message... msgs) {
		this(client);
		this.pages = new ArrayList<>();
		Collections.addAll(pages, msgs);
	}

	public PageableText(Client client, String... pages) {
		this(client);
		this.pages = new ArrayList<>();
		for (String s : pages)
			this.pages.add(new MessageBuilder(s).build());
	}

	public PageableText(Client client, List<String> pages) {
		this(client);
		this.pages = new ArrayList<>();
		for (String s : pages)
			this.pages.add(new MessageBuilder(s).build());
	}

	public void send(TextChannel channel, int page) {

		if (id != 0) {
			channel.editMessageById(id, pages.get(page)).queue();
			return;
		}

		this.page = page;
		channel.sendMessage(pages.get(page)).queue(m -> {
			this.id = m.getIdLong();
			for (String s : confirms.keySet())
				m.addReaction(s).queue();

			if (pages.size() > 3)
				m.addReaction("⏪").queue();
			if (pages.size() > 2)
				m.addReaction("⬅").queue();
			if (pages.size() > 1)
				m.addReaction("◀").queue();

			m.addReaction("❌").queue();

			if (pages.size() > 1)
				m.addReaction("▶").queue();
			if (pages.size() > 2)
				m.addReaction("➡").queue();
			if (pages.size() > 3)
				m.addReaction("⏩").queue();
		});
	}

	@SubscribeEvent
	public void onMessageReaction(GuildMessageReactionAddEvent event) {
		onGuildMessageReactionAdd(event);
	}
}
