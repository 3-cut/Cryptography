package model;

/**
 * Opérations sur des matrices d'entiers "classiques" (int[][]).
 * Ne sert pas directement à Kyber (qui manipule des matrices de polynômes,
 * int[][][] -- voir KyberModel), c'est un utilitaire générique à part.
 */
public class OpMatrices {

  public int[][] sumMatrice(int[][] a, int[][] b) {
    if (a.length != b.length || a[0].length != b[0].length) {
      System.out.println("Erreur");
      return null;
    }
    int[][] sum = new int[a.length][a[0].length];
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        sum[i][j] = a[i][j] + b[i][j];
      }
    }
    return sum;
  }

  public int[][] subMatrice(int[][] a, int[][] b) {
    if ((a.length != b.length) || (a[0].length != b[0].length)) {
      System.out.println("Erreur");
      return null;
    }
    int[][] sub = new int[a.length][a[0].length];
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        sub[i][j] = a[i][j] - b[i][j];
      }
    }
    return sub;
  }

  public int[][] multiplicationMatrice(int[][] a, int[][] b) {
    if (a[0].length != b.length) {
      System.out.println("Erreur");
      return null;
    }
    int[][] produit = new int[a.length][b[0].length];
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < b[0].length; j++) {
        produit[i][j] = 0;
        for (int k = 0; k < a[0].length; k++) {
          produit[i][j] = produit[i][j] + a[i][k] * b[k][j];
        }
      }
    }
    return produit;
  }

  public int[][] transpose(int[][] a) {
    int[][] aTranspose = new int[a[0].length][a.length];
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        aTranspose[j][i] = a[i][j];
      }
    }
    return aTranspose;
  }

  public void printMatrice(int[][] a) {
    for (int i = 0; i < a.length; i++) {
      System.out.print("[ ");
      for (int j = 0; j < a[0].length; j++) {
        System.out.print(a[i][j] + " ");
      }
      System.out.println("]\n");
    }
  }
}
