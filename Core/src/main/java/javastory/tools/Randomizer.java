package javastory.tools;

import java.util.Random;

public class Randomizer {

	private final static Random random = new Random();

	public static int nextInt() {
		return random.nextInt();
	}

	public static int nextInt(final int arg0) {
		return random.nextInt(arg0);
	}

	public static void nextBytes(final byte[] bytes) {
		random.nextBytes(bytes);
	}

	public static boolean nextBoolean() {
		return random.nextBoolean();
	}

	public static double nextDouble() {
		return random.nextDouble();
	}

	public static float nextFloat() {
		return random.nextFloat();
	}

	public static long nextLong() {
		return random.nextLong();
	}

	public static int rand(final int lbound, final int ubound) {
		return (int) (random.nextDouble() * (ubound - lbound + 1) + lbound);
	}
}