import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    private static final String EXTENSION = ".txt";

    /**
     * Функция рекурсивно обходит все папки и файлы корневой директории
     *         и пополняет наш словарь типа <название файла, его зависимости>
     * @param dirPath - абсолютный путь до корневой папки (объект типа File)
     * @param arr - словарь зависимостей, тип словаря: <название файла, строка с require>
     * @param folder_path - абсолютный путь до корневой папки (просто строка)
     * @param all_files - массив ВСЕХ файлов в корневой папке и ее подпапках
     */
    private static void listOfFiles(File dirPath, Map<String, ArrayList<String>> arr, String folder_path, ArrayList<String> all_files) throws Exception {
        File[] filesList = dirPath.listFiles();
        if (filesList == null) {
            throw new Exception("There are no files!");
        }
        for (File file : filesList) {
            if (file.isFile()) {
                try {
                    Scanner myReader = new Scanner(file);
                    String relative_name = file.getPath().replaceAll(folder_path, "");
                    while (myReader.hasNextLine()) {
                        String line = myReader.nextLine();

                        if (line.contains("require")) {  // есть ли в этой строчке зависимости
                            line = line.trim().
                                    replaceAll("require", "").
                                    replaceAll("‘", "").
                                    replaceAll("’", "XYZ").
                                    trim();  // extract нужное из строки
                            String[] requires = line.split("XYZ");  // XYZ - разделитель

                            // зависимости из текущего файла
                            ArrayList<String> dependencies = new ArrayList<String>();

                            // добавляем зависимые файлы в наш массив
                            for (String require : requires) {
                                dependencies.add(require.trim() + EXTENSION);
                            }

                            // сохраняем имя файла и его зависимости
                            ArrayList<String> temp_arr = arr.get(relative_name);
                            if (temp_arr == null) {
                                arr.put(relative_name, dependencies);
                            } else {
                                temp_arr.addAll(dependencies);
                                arr.put(relative_name, temp_arr);
                            }
                        }
                    }
                    myReader.close();
                    all_files.add(relative_name);
                } catch (Exception e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            } else if (file.isDirectory()) {
                listOfFiles(file, arr, folder_path, all_files);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // Creating a File object for directory
        String folder_path = "/Users/egorfortov/Desktop/KPO/";  // корневая директория
        File file = new File(folder_path);  // передаем путь к нужной папке с файлами (т.е. к корневой директории)

        Map<String, ArrayList<String>> states = new HashMap<>();  // <название файла, строка с require>

        ArrayList<String> all_files = new ArrayList<>();
        // Ищем все файлы в нашей корневой директории
        try {
            listOfFiles(file, states, folder_path, all_files);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        // делаем словарь вида <название файла, его уник.номер>
        Map<String, Integer> filesAndIndexes = new HashMap<>();
        int fileIndex = 0;  // уникальный индекс файла
        for(Map.Entry<String, ArrayList<String>> item : states.entrySet()){
            for (String elem: item.getValue()) {
                if (!filesAndIndexes.containsKey(elem)) {
                    filesAndIndexes.put(elem, fileIndex);
                    ++fileIndex;
                }
            }
            String elem = item.getKey();
            if (!filesAndIndexes.containsKey(elem)) {
                filesAndIndexes.put(elem, fileIndex);
                ++fileIndex;
            }
        }

        // словарь типа <индекс файла, индексы файлов от которых он зависит>
        Map<Integer, ArrayList<Integer>> mapForTopologySort = new HashMap<>();
        for(Map.Entry<String, ArrayList<String>> item : states.entrySet()){
            ArrayList<Integer> indexes = new ArrayList<Integer>();  // массив "правых" индексов
            String value = item.getKey();
            int file_index = filesAndIndexes.get(value);
            for (int i = 0; i < item.getValue().size(); ++i) {
                String str = item.getValue().get(i);
                int inner_file_index = filesAndIndexes.get(str);
                indexes.add(inner_file_index);
            }
            mapForTopologySort.put(file_index, indexes);
        }

        // теперь можем работать со словарем mapForTopologySort, используем его
        // для топологической сортировки и проверки на цикличность нашего графа

        ////////////////////////////// KEY ALGORITHM //////////////////////////////
        int V = filesAndIndexes.size();  // количество вершин в графе = количество файлов
        Graph files_graph = new Graph(V);

        // вводим однонаправленные ребра графа - НАЧАЛО
        for(Map.Entry<Integer,  ArrayList<Integer>> item : mapForTopologySort.entrySet()) {
            for (int i = 0; i < item.getValue().size(); ++i) {
                int index2 = item.getValue().get(i);
                files_graph.addEdge(item.getKey(), index2);
            }
        }
        // вводим однонаправленные ребра графа - КОНЕЦ

        if (files_graph.isCyclic()) {
            System.out.println("\nГраф цикличен, невозможно выдать требуемый порядок файлов!");
            return;
        }
        
        // graph is Acyclic => make topology sort
        // "Топологическая сортировка" графа:
        Vector<Integer> vec = new Vector<>();  // нужный нам порядок
        files_graph.topologicalSort(vec);  // нужный нам порядок

        ArrayList<String> all_printed_files = new ArrayList<>();

        // соотносим индексы их файлам
        System.out.println("###########################################");
        System.out.println("Итоговый порядок файлов:");
        for (int i = 0; i < vec.size(); ++i) {
            final int j = i;
            filesAndIndexes.forEach((key, value) -> {
                if (value.equals(vec.get(j))) {
                    if (!all_printed_files.contains(key)) {
                        all_printed_files.add(key);
                        System.out.println(key);
                    }
                }
            });
        }
        
        all_files.removeAll(all_printed_files);
        // вывод всех файлов, которые ни от кого не зависят и их нет
        // в зависимостях всех выведенных файлов
        for (String allFile : all_files) {
            if (!all_printed_files.contains(allFile)) {
                System.out.println(allFile);
                all_printed_files.add(allFile);
            }
        }
        // теперь в all_printed_files лежат все файлы в нужном нам порядке

        System.out.println("###########################################\n\n");
        System.out.println("###########################################");
        System.out.println("Сконкатенированные файлы:");
        for (String allPrintedFile : all_printed_files) {
            // пропускаем скрытые файлы, сгенерированные mac os
            if (allPrintedFile.contains("DS_Store")) {
                continue;
            }
            Path filePath = Path.of(folder_path + allPrintedFile);
            String content = "CAN NOT READ THIS FILE \n";
            try {
                content = Files.readString(filePath);
            } catch (Exception ignored) {
            }
            ;  // ошибку никак не обрабатываем, продолжаем читать файлы дальше
            System.out.println(content);
        }
        System.out.println("###########################################");
    }
}