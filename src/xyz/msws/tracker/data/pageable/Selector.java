package xyz.msws.tracker.data.pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.utils.MSG;

public class Selector<T> {
	private List<T> options;
	private List<Callback<T>> actions = new ArrayList<>();

	public Selector(Collection<T> options) {
		this.options = new ArrayList<>(options);
	}

	public void filter(Predicate<? super T> filter) {
		this.options = options.stream().filter(filter).collect(Collectors.toList());
	}

	public void sort(Comparator<? super T> sorter) {
		options.sort(sorter);
	}

	public void sortLexi(String source) {
		sort(new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				int s1 = MSG.simplify(o1.toString().replace(" ", "")).compareTo(source);
				int s2 = MSG.simplify(o2.toString().replace(" ", "")).compareTo(source);
				return s1 == s2 ? 0 : s1 > s2 ? -1 : 1;
			}
		});
	}

	public void send(Client client, Message message) {
		if (options.size() == 1) {
			select(options.get(0));
			return;
		}
		if (options.isEmpty()) {
			select(null);
		}

		List<String> messages = new ArrayList<>();

		int size = 5;

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < options.size(); i++) {
			builder.append((i % size) + 1).append(": ").append(options.get(i).toString()).append("\n");
			if ((i + 1) % size == 0) {
				messages.add(builder.toString());
				builder = new StringBuilder();
			}
		}
		if (builder.length() != 0)
			messages.add(builder.toString());

		String[] nums = new String[] { "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣" };

		Pageable<?> pager = new PageableText(client, messages).bindTo(message.getAuthor());

		for (int i = 0; i < Math.min(nums.length, options.size()); i++) {
			final int fi = i;
			pager.addCallback(nums[i], new Callback<GuildMessageReactionAddEvent>() {
				@Override
				public void execute(GuildMessageReactionAddEvent call) {
					select(options.get(fi + (pager.getPage() * size)));
				}
			});
		}
		pager.send(message.getTextChannel());
	}

	private void select(T opt) {
		actions.forEach(o -> o.execute(opt));
	}

	public void setAction(Callback<T> action) {
		actions.clear();
		actions.add(action);
	}

	public void addAction(Callback<T> action) {
		actions.add(action);
	}

	public void clearActions(Callback<T> action) {
		actions.clear();
	}

}
