package com.alicewolves.application.tileserver.controller;

import com.jhlabs.image.ChannelMixFilter;
import com.jhlabs.image.MaskFilter;
import com.jhlabs.image.RaysFilter;
import com.sleepycat.je.DatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.IndexColorModel;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.util.Map;

@RestController
@Slf4j
public class TileController {
  private static final byte[] COLORS = {0,//
          (byte) 0xff, (byte) 0xff, (byte) 0xff, // white
          (byte) 0xff, (byte) 0x00, (byte) 0x00 // red
  };
  private static final IndexColorModel COLORMODEL = new IndexColorModel(8, 2, COLORS, 1, false);
  private static final Font FONT_LARGE = new Font("Sans Serif", Font.BOLD, 30);
  private static final Font FONT_SMALL = new Font("Sans Serif", Font.BOLD, 20);
  @Autowired
  TileStoreService service;
  @Value("${tiles.debug: false}")
  private boolean debug;

  public static BufferedImage imageToBufferedImage(Image image) {

    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = bufferedImage.createGraphics();
    g2.drawImage(image, 0, 0, null);
    g2.dispose();

    return bufferedImage;

  }

  @GetMapping("/clear/{map}/{z:\\d*}/{x:\\d*}/{y:\\d*}{exp}")
  public void clearTiles(@PathVariable String map,
                         @PathVariable int z,
                         @PathVariable int x,
                         @PathVariable int y,
                         @PathVariable String exp,
                         @RequestParam Map<String, String> params) {
    service.clearCache(map, z, x, y);
  }

  @GetMapping("/clearall")
  public void clearTiles(@PathVariable String map) {
    service.clearAllCache(map);
  }

  @GetMapping("/{map}/tile/{z:\\d*}/{x:\\d*}/{y:\\d*}{exp}")
  public void getTiles(@PathVariable String map,
                       @PathVariable int z,
                       @PathVariable int x,
                       @PathVariable int y,
                       @PathVariable String exp,
                       @RequestParam Map<String, String> params,
                       HttpServletRequest request, HttpServletResponse response) throws IOException, DatabaseException {

    BufferedImage tile = service.getTile(map, z, x, y);
    if (tile == null) {
      tile = generateImage(map, z, x, y);
    }
//    if ("Gray".equalsIgnoreCase(params.get("style"))) {
//      tile = doColorGray(tile);
//    } else if ("Linear".equalsIgnoreCase(params.get("style"))) {
//      tile = doLinearColor(tile);
//    } else if ("blue".equalsIgnoreCase(params.get("style"))) {
//      tile = makeColorTransparent(tile, Color.RED, Color.BLUE);
//    } else if ("red".equalsIgnoreCase(params.get("style"))) {
//      tile = makeColorTransparent(tile, Color.BLUE, Color.RED);
//    }

//    ChannelMixFilter filter = new ChannelMixFilter();
//    filter.setBlueGreen(3);
//    filter.setGreenRed(2);
//    filter.setRedBlue(1);
//    filter.setIntoB(140);
//    filter.setIntoG(200);
//    filter.setIntoR(180);
//    tile = filter.filter(tile,null);

    ImageIO.write(tile, "png", response.getOutputStream());
    response.flushBuffer();
  }
//
//  protected BufferedImage doColorGray(BufferedImage bi) {
//    ColorConvertOp filterObj = new ColorConvertOp(
//            ColorSpace.getInstance(ColorSpace.CS_GRAY), null
//    );
//    return filterObj.filter(bi, null);
//  }
//
//  public BufferedImage makeColorTransparent(BufferedImage im, final Color mul, final Color add) {
//    ImageFilter filter = new RGBImageFilter() {
//
//      public int mR = mul.getRGB() | 0x00FF0000 >> 16;
//      public int mG = mul.getRGB() | 0x00FF0000 >> 8;
//      public int mB = mul.getRGB() | 0x00FF0000;
//
//      public int aR = add.getRGB() | 0x00FF0000 >> 16;
//      public int aG = add.getRGB() | 0x00FF0000 >> 8;
//      public int aB = add.getRGB() | 0x00FF0000;
//
//
//      @Override
//      public final int filterRGB(int x, int y, int rgb) {
//        int r = 0x00FF0000 | rgb >> 16;
//        int g = 0x0000FF00 | rgb >> 8;
//        int b = 0x000000FF | rgb;
//        r = r * mR / 0x00FF + aR;
//        g = g * mG / 0x00FF + aG;
//        b = b * mB / 0x00FF + aB;
//        rgb = (r << 16) | (g << 8) | b;
//        return 0x00FFFFFF | rgb;
//      }
//    };
//
//    return imageToBufferedImage(Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(im.getSource(), filter)));
//  }
//
//  protected BufferedImage doLinearColor(BufferedImage bi) {
//    ColorConvertOp filterObj = new ColorConvertOp(
//            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), null
//    );
//    return filterObj.filter(bi, null);
//  }

  protected BufferedImage generateImage(String map, int z, int x, int y) {
    BufferedImage tile = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_INDEXED, COLORMODEL);
    if (!debug) {
      return tile;
    }
    Graphics2D g2 = tile.createGraphics();
    try {
      g2.setColor(Color.WHITE);
      g2.fillRect(0, 0, 255, 255);
      g2.setColor(Color.RED);
      g2.drawRect(0, 0, 255, 255);
      g2.drawLine(0, 0, 255, 255);
      g2.drawLine(255, 0, 0, 255);

      int py = 40;
      g2.setFont(FONT_SMALL);
      g2.drawString(String.format("[%s]", map), 8, py);
      py += 35;
      g2.drawString(String.format("z:%d, x:%d, y:%d", z, x, y), 8, py);
    } finally {
      g2.dispose();
    }
    return tile;
  }
}
