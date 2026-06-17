package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.product.ProductRequest;
import com.tps.dto.product.ProductResponse;
import com.tps.entity.Notification;
import com.tps.entity.Product;
import com.tps.entity.ProductImage;
import com.tps.entity.Report;
import com.tps.entity.User;
import com.tps.exception.BusinessException;
import com.tps.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final FileService fileService;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final SensitiveWordService sensitiveWordService;

    @Transactional
    public ProductResponse create(Long userId, ProductRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (Boolean.TRUE.equals(user.getPublishBanned())) {
            throw new IllegalArgumentException("账号已被禁止发布商品，请联系管理员");
        }
        validateProductContent(req);
        Product product = new Product();
        product.setUserId(userId);
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setCategory(req.getCategory());
        product.setCondition(req.getCondition());
        product.setLocation(req.getLocation());
        productRepository.save(product);
        if (req.getImageUrls() != null) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProductId(product.getId());
                img.setImageUrl(req.getImageUrls().get(i));
                img.setSortOrder(i);
                productImageRepository.save(img);
            }
        }
        return toResponse(product, userId);
    }

    public Page<ProductResponse> list(int page, int size, Long currentUserId) {
        // 商品列表优先看最近擦亮，其次看发布时间，贴合二手交易首页的曝光逻辑。
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt")));
        return productRepository.findByStatus(Product.ProductStatus.ON_SALE, pageable)
                .map(p -> toResponse(p, currentUserId));
    }

    public Page<ProductResponse> search(String keyword, String category, BigDecimal minPrice,
                                        BigDecimal maxPrice, String condition, int page, int size, Long currentUserId) {
        if (sensitiveWordService.containsSensitive(keyword)) {
            throw new IllegalArgumentException(SensitiveWordService.SEARCH_MESSAGE);
        }
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Product.ProductStatus.ON_SALE));
            if (keyword != null && !keyword.isBlank()) {
                List<Predicate> keywordPredicates = new ArrayList<>();
                for (String term : sensitiveWordService.expandSearchTerms(keyword)) {
                    String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                    keywordPredicates.add(cb.like(cb.lower(root.get("title")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("description")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("location")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("category")), pattern));
                }
                if (!keywordPredicates.isEmpty()) {
                    predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
                }
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (condition != null && !condition.isBlank()) {
                predicates.add(cb.equal(root.get("condition"), Product.Condition.valueOf(condition)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt")));
        return productRepository.findAll(spec, pageable).map(p -> toResponse(p, currentUserId));
    }

    @Transactional
    public ProductResponse getDetail(Long productId, Long currentUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        productRepository.incrementViewCount(productId);
        return toResponse(product, currentUserId);
    }

    public ProductResponse getDetailWithoutIncrement(Long productId, Long currentUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        return toResponse(product, currentUserId);
    }

    @Transactional
    public ProductResponse update(Long userId, Long productId, ProductRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        validateProductContent(req);
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setCategory(req.getCategory());
        product.setCondition(req.getCondition());
        product.setLocation(req.getLocation());
        productRepository.save(product);
        if (req.getImageUrls() != null) {
            productImageRepository.deleteByProductId(productId);
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProductId(productId);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setSortOrder(i);
                productImageRepository.save(img);
            }
        }
        return toResponse(product, userId);
    }

    @Transactional
    public void updateStatus(Long userId, Long productId, Product.ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        if (status == Product.ProductStatus.SOLD) {
            throw BusinessException.conflict("商品成交状态只能由订单流程更新");
        }
        if (product.getStatus() == Product.ProductStatus.SOLD && status != Product.ProductStatus.SOLD) {
            throw BusinessException.conflict("已售出商品不能重新上架");
        }
        if (product.getStatus() == status) return;
        product.setStatus(status);
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse bump(Long userId, Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        if (product.getStatus() != Product.ProductStatus.ON_SALE) {
            throw new IllegalArgumentException("只有在售商品可以擦亮");
        }

        // 擦亮次数按天重置，并且锁定当前商品记录，避免并发点击把次数冲破每日上限。
        LocalDate today = LocalDate.now();
        if (!today.equals(product.getLastBumpDate())) {
            product.setLastBumpDate(today);
            product.setBumpCountToday(0);
        }
        int count = product.getBumpCountToday() == null ? 0 : product.getBumpCountToday();
        if (count >= 3) {
            throw new IllegalArgumentException("每件商品每天最多擦亮3次");
        }
        product.setBumpCountToday(count + 1);
        product.setBumpedAt(LocalDateTime.now());
        productRepository.save(product);
        return toResponse(product, userId);
    }

    public List<ProductResponse> myProducts(Long userId) {
        return productRepository.findByUserId(userId).stream()
                .map(p -> toResponse(p, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void report(Long userId, Long productId, String reason, List<String> evidenceImageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能举报自己的商品");
        }
        if (reportRepository.existsByReporterIdAndProductIdAndStatus(userId, productId, Report.ReportStatus.PENDING)) {
            return;
        }
        sensitiveWordService.rejectIfSensitiveFields(
                sensitiveWordService.field("reason", "举报原因", reason)
        );
        Report report = new Report();
        report.setReporterId(userId);
        report.setProductId(productId);
        report.setReason(reason);
        report.setEvidenceImageUrls(serializeEvidenceImageUrls(evidenceImageUrls));
        reportRepository.save(report);

        Notification notification = new Notification();
        notification.setUserId(product.getUserId());
        notification.setType("REPORT");
        notification.setTitle("商品被举报");
        notification.setContent("你的商品被用户举报，平台将进行审核");
        notificationRepository.save(notification);
    }

    private String serializeEvidenceImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        return urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .limit(3)
                .collect(Collectors.joining(","));
    }

    private void validateProductContent(ProductRequest req) {
        sensitiveWordService.rejectIfSensitiveFields(
                sensitiveWordService.field("title", "标题", req.getTitle()),
                sensitiveWordService.field("description", "描述", req.getDescription()),
                sensitiveWordService.field("category", "分类", req.getCategory()),
                sensitiveWordService.field("location", "交易地点", req.getLocation())
        );
    }

    public ProductResponse toResponse(Product p, Long currentUserId) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setUserId(p.getUserId());
        userRepository.findById(p.getUserId()).ifPresent(u -> {
            r.setSellerNickname(u.getNickname());
            r.setSellerAvatar(fileService.toAbsoluteUrl(u.getAvatarUrl()));
            r.setSellerCreditScore(u.getCreditScore());
            r.setSellerReviewCount(reviewRepository.countByRevieweeId(u.getId()));
        });
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        r.setPrice(p.getPrice());
        r.setCategory(p.getCategory());
        r.setCondition(p.getCondition() != null ? p.getCondition().name() : null);
        r.setStatus(p.getStatus().name());
        r.setLocation(p.getLocation());
        r.setViewCount(p.getViewCount());
        r.setFavoriteCount(p.getFavoriteCount());
        r.setBumpedAt(p.getBumpedAt());
        r.setTakedownReason(p.getTakedownReason());
        r.setTakedownBy(p.getTakedownBy());
        r.setTakedownAt(p.getTakedownAt());
        r.setCreatedAt(p.getCreatedAt());
        List<String> urls = productImageRepository.findByProductIdOrderBySortOrder(p.getId())
                .stream().map(ProductImage::getImageUrl).map(fileService::toAbsoluteUrl).collect(Collectors.toList());
        r.setImageUrls(urls);
        if (currentUserId != null) {
            r.setFavorited(favoriteRepository.existsByUserIdAndProductId(currentUserId, p.getId()));
        }
        return r;
    }
}
