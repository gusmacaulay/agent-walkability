package org.mccaughey.geotools.util;

public class Pair<E,F> {
	private E one;
	private F two;
	public Pair() {
		// TODO Auto-generated constructor stub
	}
	public Pair(E one, F two) {
		super();
		this.one = one;
		this.two = two;
	}
	public E getOne() {
		return one;
	}
	public void setOne(E one) {
		this.one = one;
	}
	public F getTwo() {
		return two;
	}
	public void setTwo(F two) {
		this.two = two;
	}
	 

}
