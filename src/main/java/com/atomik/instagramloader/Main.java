package com.atomik.instagramloader;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.media.ImageVersions;
import com.github.instagram4j.instagram4j.models.media.timeline.CarouselItem;
import com.github.instagram4j.instagram4j.models.media.timeline.ImageCarouselItem;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineCarouselMedia;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineImageMedia;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineVideoMedia;
import com.github.instagram4j.instagram4j.models.media.timeline.VideoCarouselItem;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserRequest;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Wrong args");
            System.out.println("Use this app in that way:");
            System.out.println("java -jar InstaLoader.jar <yourUsername> <yourPassword> <targetUsername> <folderToStore>");
            System.exit(1);
        }

        IGClient instagramClient = IGClient.builder()
                .username(args[0])
                .password(args[1])
                .login();

        AtomicLong userPk = new AtomicLong();
        instagramClient.actions().users().findByUsername(args[2]).thenAccept(response -> {
            userPk.set(response.getUser().getPk());
        }).join();
        FeedUserRequest request = new FeedUserRequest(userPk.get());
        FeedUserResponse response = instagramClient.sendRequest(request).join();
        List<TimelineMedia> items = response.getItems();
        List<String> urls = getUrls(items);

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            downloadFile(instagramClient.getHttpClient(), url, args[3] + "/" + i + getExtension(url));
        }
    }

    public static String getExtension(String url) {
        String urlWithoutFields = url.substring(0, url.indexOf('?'));
        String[] parts = urlWithoutFields.split("\\.");
        return parts[parts.length - 1];
    }

    public static void downloadFile(OkHttpClient client, String url, String destinationPath) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        InputStream is = response.body().byteStream();
        BufferedInputStream input = new BufferedInputStream(is);

        File destinationFile = new File(destinationPath);
        OutputStream output = new FileOutputStream(destinationFile);

        byte[] data = new byte[1024];

        int count = 0;
        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();
    }

    public static String extractUrlFromImageVersions(ImageVersions imageVersions) {
        return imageVersions.getCandidates().get(0).getUrl();
    }

    public static List<String> getUrls(List<TimelineMedia> timelineMedias) {
        List<String> urls = new ArrayList<>();
        for (TimelineMedia  timelineMedia : timelineMedias) {
            if (timelineMedia instanceof TimelineImageMedia timelineImageMedia) {
                urls.add(extractUrlFromImageVersions(timelineImageMedia.getImage_versions2()));
            } else if (timelineMedia instanceof TimelineVideoMedia timelineVideoMedia) {
                urls.add(extractUrlFromImageVersions(timelineVideoMedia.getImage_versions2()));
            } else if (timelineMedia instanceof TimelineCarouselMedia carousel) {
                List<CarouselItem> carouselItems = carousel.getCarousel_media();
                for (CarouselItem carouselItem : carouselItems) {
                    if (carouselItem instanceof ImageCarouselItem imageCarouselItem) {
                        urls.add(extractUrlFromImageVersions(imageCarouselItem.getImage_versions2()));
                    } else if (carouselItem instanceof VideoCarouselItem videoCarouselItem) {
                        urls.add(extractUrlFromImageVersions(videoCarouselItem.getImage_versions2()));
                    }
                }
            }
        }
        return urls;
    }
}
