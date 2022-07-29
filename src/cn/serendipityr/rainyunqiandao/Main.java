package cn.serendipityr.rainyunqiandao;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static String version = "1.1";
    public static Integer maxDelay = null;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(getTimeStr() + "------------------------------------------------------------------");
        System.out.println(getTimeStr() + "雨云签到工具 " + version + " by SerendipityR ~");
        System.out.println(getTimeStr() + "Github发布页: https://github.com/SerendipityR-2022/Rainyun-Qiandao");
        System.out.println(getTimeStr() + "------------------------------------------------------------------");

        if (args.length != 2 && args.length != 3) {
            System.out.println(getTimeStr() + "无法执行任务，参数不正确！");
            System.out.println(getTimeStr() + "正确用法: java -jar ***.jar (用户名) (密码) [随机延时(分钟)]");
            System.exit(0);
        }

        String username = args[0];
        String password = args[1];

        if (args.length == 3) {
            maxDelay = Integer.parseInt(args[2]);
        }

        if (maxDelay != null && maxDelay > 0) {
            Integer delay = getRandomNumber(0, maxDelay);
            System.out.println(getTimeStr() + "本次任务将延时" + delay + "分钟执行。");
            Thread.sleep(delay * 60 * 1000);
        }

        start(username, password);
    }

    public static void start(String username, String password) {
        Map<String, String> loginHeaders = new HashMap<>();
        Map<String, String> result_1 = doLogin(username, password);

        if (result_1.containsKey("Error")) {
            System.out.println(getTimeStr() + "登录失败: " + result_1.get("Error"));
            System.exit(0);
        } else {
            System.out.println(getTimeStr() + "登录成功: " + result_1.get("result"));
            System.out.println(getTimeStr() + "Cookies: " + result_1.get("cookie"));

            loginHeaders.put("cookie", result_1.get("cookie"));
        }

        Map<String, String> result_2 = doQiandao(loginHeaders);

        if (result_2.containsKey("Error")) {
            if (result_2.get("Error").contains("未达到完成这个任务的条件，继续加油哦！")) {
                System.out.println(getTimeStr() + "签到结果: 今天已经签过到了哦~");
            } else {
                System.out.println(getTimeStr() + "签到失败: " + result_2.get("Error"));
            }
        } else {
            if (result_2.get("result").contains("ok")) {
                System.out.println(getTimeStr() + "签到结果: " + "成功！");
            } else {
                System.out.println(getTimeStr() + "签到结果: " + result_2.get("result"));
            }
        }

        System.exit(0);
    }

    public static Map<String, String> doLogin(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("field", username);
        params.put("password", password);

        Map<String, String> headers = new HashMap<>();
        headers.put("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");

        return doPost("https://api.v2.rainyun.com/user/login", params, headers);
    }

    public static Map<String, String> doQiandao(Map<String, String> loginHeaders) {
        Map<String, String> params = new HashMap<>();
        params.put("task_name", "每日签到");
        params.put("verifyCode", "");

        String CSRFToken = getCSRFToken(loginHeaders.get("cookie"));
        System.out.println(getTimeStr() + "CSRFToken: " + CSRFToken);

        Map<String, String> headers = new HashMap<>();
        headers.put("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        headers.put("x-csrf-token", CSRFToken);
        headers.putAll(loginHeaders);

        return doPost("https://api.v2.rainyun.com/user/reward/tasks", params, headers);
    }

    public static String getCSRFToken(String cookies) {
        String[] paths = cookies.split("; ");

        for (String path:paths) {
            if (path.contains("X-CSRF-Token")) {
                return path.split("=")[1];
            }
        }

        return "";
    }

    public static Integer getRandomNumber(int Min,int Max) {
        return (int) (Math.random()*(Min-Max)+Max);
    }

    public static Map<String, String> doPost(String URL, Map<String, String> params, Map<String, String> headers) {
        BufferedReader in;
        OutputStreamWriter out;
        HttpURLConnection conn;
        StringBuilder result = new StringBuilder();
        Map<String,String> final_result = new HashMap<>();

        try {
            java.net.URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            //发送POST请求必须设置为true
            conn.setDoOutput(true);
            conn.setDoInput(true);
            //设置连接超时时间和读取超时时间
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Host", url.getHost());
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Accept-Language", "zh,zh-CN;q=0.9,en;q=0.8,zh-HK;q=0.7,ja;q=0.6,ru;q=0.5,de;q=0.4");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

            if (headers != null) {
                for (String header:headers.keySet()) {
                    conn.setRequestProperty(header, headers.get(header));
                }
            }

            //获取输出流
            out = new OutputStreamWriter(conn.getOutputStream());
            String jsonStr = "{}";

            if (params != null) {
                jsonStr = JSONObject.toJSONString(params);
            }

            out.write(jsonStr);
            out.flush();
            out.close();

            //取得输入流，并使用Reader读取
            if (200 == conn.getResponseCode()) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }

                final_result.put("Error", conn.getResponseCode() + " | " + result);
                return final_result;
            }

            in.close();
        } catch (Exception e) {
            final_result.put("Error", "无法连接到服务器 | " + e);
            return final_result;
        }

        final_result.put("result", result.toString());
        StringBuilder cookies = new StringBuilder();

        try {
            for (String cookie:conn.getHeaderFields().get("Set-Cookie")) {
                if (cookies.toString().contains(";")) {
                    cookies.append("; ").append(cookie);
                } else {
                    cookies.append(cookie).append("; ");
                }
            }

            final_result.put("cookie", cookies.toString());
        } catch (Exception e) {
            final_result.put("cookie", null);
        }

        return final_result;
    }

    public static String getTimeStr() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

        return "[" + df.format(date) + "] ";
    }
}
