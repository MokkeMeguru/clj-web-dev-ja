public class AddClass {
  public static int add(int x, int y) { return x + y; }
  public static void main(String[] args) {
    int x = 1;
    int y = 2;

    int z = add(x, y);
    System.out.printf("result is: %d\n", z);
  }
}
