package xyz.msws.tracker.data.pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import xyz.msws.tracker.Client;

public class PageableEmbed extends Pageable<MessageEmbed> {

	private PageableEmbed(Client client) {
		super(client);
	}

	public PageableEmbed(Client client, Collection<MessageEmbed> pages) {
		this(client);
		this.pages = new ArrayList<>(pages);
	}

	public PageableEmbed(Client client, MessageEmbed... msgs) {
		this(client);
		this.pages = new ArrayList<>();
		Collections.addAll(pages, msgs);
	}

	public PageableEmbed(Client client, String... pages) {
		this(client);
		this.pages = new ArrayList<>();
		for (String s : pages)
			this.pages.add(new EmbedBuilder().setDescription(s).build());
	}

	public PageableEmbed(Client client, List<String> pages) {
		this(client);
		this.pages = new ArrayList<>();
		for (String s : pages)
			this.pages.add(new EmbedBuilder().setDescription(s).build());
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
