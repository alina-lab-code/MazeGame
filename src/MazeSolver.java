import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MazeSolver {
    public static List<Point> findPath(boolean[][] walls, int width, int height) {
        List<Point> path = new ArrayList<>();

        if (walls[0][0] || walls[width - 1][height - 1]) return path;

        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[width][height];
        Point[][] parent = new Point[width][height];

        Point start = new Point(0, 0);
        Point end = new Point(width - 1, height - 1);

        queue.add(start);
        visited[0][0] = true;


        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        boolean pathFound = false;
        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.x == end.x && current.y == end.y) {
                pathFound = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (!walls[nx][ny] && !visited[nx][ny]) {
                        visited[nx][ny] = true;
                        parent[nx][ny] = current;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }


        if (pathFound) {
            Point curr = end;
            while (curr != null) {
                path.add(0, curr);
                curr = parent[curr.x][curr.y];
            }
        }
        return path;
    }
}