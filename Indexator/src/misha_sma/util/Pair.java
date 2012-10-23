package misha_sma.util;

public class Pair<L, R> {
	private L left;
	private R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public int hashCode() {
		int hashLeft = left != null ? left.hashCode() : 0;
		int hashRight = right != null ? right.hashCode() : 0;

		return (hashLeft + hashRight) * hashRight + hashLeft;
	}

	public boolean equals(Object other) {
		if (other instanceof Pair) {
			Pair otherPair = (Pair) other;
			return ((this.left == otherPair.left || (this.left != null && otherPair.left != null && this.left
					.equals(otherPair.left))) && (this.right == otherPair.right || (this.right != null
					&& otherPair.right != null && this.right.equals(otherPair.right))));
		}

		return false;
	}

	public String toString() {
		return "(" + left + ", " + right + ")";
	}

	public L getLeft() {
		return left;
	}

	public R getRight() {
		return right;
	}
}
