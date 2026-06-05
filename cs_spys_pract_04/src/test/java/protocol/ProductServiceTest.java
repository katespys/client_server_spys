package protocol;

import org.junit.jupiter.api.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductService — CRUD та пошук")
class ProductServiceTest {

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService();
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("створює товар з коректними даними")
        void createValid() {
            Product p = service.create("Молоко", "Молочне", 100, 45.5);

            assertThat(p.getId()).isNotBlank();
            assertThat(p.getName()).isEqualTo("Молоко");
            assertThat(p.getCategory()).isEqualTo("Молочне");
            assertThat(p.getQuantity()).isEqualTo(100);
            assertThat(p.getPrice()).isEqualTo(45.5);
        }

        @Test
        @DisplayName("кожен новий товар отримує унікальний id")
        void createUniqueIds() {
            Product a = service.create("А", "кат", 1, 1.0);
            Product b = service.create("Б", "кат", 2, 2.0);
            assertThat(a.getId()).isNotEqualTo(b.getId());
        }

        @Test
        @DisplayName("порожня назва — IllegalArgumentException")
        void createEmptyName() {
            assertThatThrownBy(() -> service.create("", "кат", 1, 1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("від'ємна кількість — IllegalArgumentException")
        void createNegativeQuantity() {
            assertThatThrownBy(() -> service.create("Товар", "кат", -5, 1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("від'ємна ціна — IllegalArgumentException")
        void createNegativePrice() {
            assertThatThrownBy(() -> service.create("Товар", "кат", 5, -1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById()")
    class ReadTests {

        @Test
        @DisplayName("знаходить існуючий товар")
        void findExisting() {
            Product created = service.create("Хліб", "Хлібобулочне", 50, 20.0);
            Optional<Product> found = service.findById(created.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Хліб");
        }

        @Test
        @DisplayName("повертає порожній Optional для неіснуючого id")
        void findNonExistent() {
            Optional<Product> found = service.findById("non-existent-id");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("оновлює всі поля")
        void updateAllFields() {
            Product created = service.create("Старий", "кат1", 10, 5.0);
            Product updated = service.update(created.getId(), "Новий", "кат2", 99, 15.0);

            assertThat(updated.getName()).isEqualTo("Новий");
            assertThat(updated.getCategory()).isEqualTo("кат2");
            assertThat(updated.getQuantity()).isEqualTo(99);
            assertThat(updated.getPrice()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("часткове оновлення — null-поля не змінюються")
        void updatePartial() {
            Product created = service.create("Молоко", "Молочне", 100, 30.0);
            Product updated = service.update(created.getId(), null, null, 200, null);

            assertThat(updated.getName()).isEqualTo("Молоко");
            assertThat(updated.getCategory()).isEqualTo("Молочне");
            assertThat(updated.getQuantity()).isEqualTo(200);
            assertThat(updated.getPrice()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("оновлення неіснуючого id — IllegalArgumentException")
        void updateNotFound() {
            assertThatThrownBy(() -> service.update("bad-id", "X", "Y", 1, 1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("від'ємна кількість при оновленні — IllegalArgumentException")
        void updateNegativeQuantity() {
            Product created = service.create("Товар", "кат", 10, 5.0);
            assertThatThrownBy(() -> service.update(created.getId(), null, null, -1, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("видаляє існуючий товар — повертає true")
        void deleteExisting() {
            Product created = service.create("Масло", "Молочне", 10, 60.0);
            boolean deleted = service.delete(created.getId());

            assertThat(deleted).isTrue();
            assertThat(service.findById(created.getId())).isEmpty();
        }

        @Test
        @DisplayName("видалення неіснуючого id — повертає false")
        void deleteNonExistent() {
            assertThat(service.delete("bad-id")).isFalse();
        }
    }

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @BeforeEach
        void populate() {
            service.create("Молоко 1л",  "Молочне",  200, 45.0);
            service.create("Молоко 2л",  "Молочне",   80, 75.0);
            service.create("Кефір",      "Молочне",   50, 35.0);
            service.create("Хліб білий", "Хлібобулочне", 300, 18.0);
            service.create("Хліб чорний","Хлібобулочне", 150, 22.0);
            service.create("Яблуко",     "Фрукти",   500,  8.5);
            service.create("Груша",      "Фрукти",   100, 12.0);
        }

        @Test
        @DisplayName("без фільтрів — повертає всі товари")
        void noFilters() {
            ProductFilter f = ProductFilter.builder().pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getTotalItems()).isEqualTo(7);
        }

        @Test
        @DisplayName("фільтр за категорією")
        void filterByCategory() {
            ProductFilter f = ProductFilter.builder().category("Молочне").pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getTotalItems()).isEqualTo(3);
            assertThat(result.getItems()).allMatch(p -> p.getCategory().equals("Молочне"));
        }

        @Test
        @DisplayName("фільтр за назвою (підрядок, регістронезалежний)")
        void filterByName() {
            ProductFilter f = ProductFilter.builder().name("молоко").pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getTotalItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("фільтр мінімальна ціна")
        void filterByMinPrice() {
            ProductFilter f = ProductFilter.builder().minPrice(40.0).pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getItems()).allMatch(p -> p.getPrice() >= 40.0);
            assertThat(result.getTotalItems()).isEqualTo(2); // 45.0, 75.0
        }

        @Test
        @DisplayName("фільтр максимальна ціна")
        void filterByMaxPrice() {
            ProductFilter f = ProductFilter.builder().maxPrice(20.0).pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getItems()).allMatch(p -> p.getPrice() <= 20.0);
            assertThat(result.getTotalItems()).isEqualTo(3); // 18.0, 8.5, 12.0
        }

        @Test
        @DisplayName("фільтр мін-макс кількість")
        void filterByQuantityRange() {
            ProductFilter f = ProductFilter.builder()
                    .minQuantity(100).maxQuantity(200)
                    .pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getItems()).allMatch(p -> p.getQuantity() >= 100 && p.getQuantity() <= 200);
            assertThat(result.getTotalItems()).isEqualTo(3); // 200, 80 ні, 150, 100
        }

        @Test
        @DisplayName("комбінований фільтр: назва + категорія")
        void filterByNameAndCategory() {
            ProductFilter f = ProductFilter.builder()
                    .name("хліб").category("Хлібобулочне")
                    .pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getTotalItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("комбінований фільтр: категорія + мінімальна ціна")
        void filterByCategoryAndMinPrice() {
            ProductFilter f = ProductFilter.builder()
                    .category("Молочне").minPrice(40.0)
                    .pageSize(100).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getTotalItems()).isEqualTo(2); // 45.0, 75.0
        }

        @Test
        @DisplayName("пагінація — перша сторінка")
        void paginationFirstPage() {
            ProductFilter f = ProductFilter.builder().page(0).pageSize(3).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getItems()).hasSize(3);
            assertThat(result.getTotalItems()).isEqualTo(7);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("пагінація — остання неповна сторінка")
        void paginationLastPage() {
            ProductFilter f = ProductFilter.builder().page(2).pageSize(3).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getItems()).hasSize(1); // 7 % 3 = 1
        }

        @Test
        @DisplayName("пагінація — сторінка за межами — порожній список")
        void paginationOutOfBounds() {
            ProductFilter f = ProductFilter.builder().page(99).pageSize(3).build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("пошук без результатів — totalItems = 0")
        void noResults() {
            ProductFilter f = ProductFilter.builder().name("не існує").build();
            PageResult<Product> result = service.search(f);
            assertThat(result.getTotalItems()).isEqualTo(0);
            assertThat(result.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ProductSerializer")
    class SerializerTests {

        @Test
        @DisplayName("productToJson → productFromJson — roundtrip зберігає всі поля")
        void roundtripProduct() {
            Product original = new Product("test-id", "Сир", "Молочне", 30, 89.9);
            String json = ProductSerializer.productToJson(original);
            Product restored = ProductSerializer.productFromJson(json);

            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getName()).isEqualTo(original.getName());
            assertThat(restored.getCategory()).isEqualTo(original.getCategory());
            assertThat(restored.getQuantity()).isEqualTo(original.getQuantity());
            assertThat(restored.getPrice()).isEqualTo(original.getPrice());
        }

        @Test
        @DisplayName("filterFromJson парсить динамічний фільтр — лише name і category")
        void filterFromJsonPartial() {
            String json = "{\"name\":\"молоко\",\"category\":\"Молочне\",\"page\":0,\"pageSize\":10}";
            ProductFilter f = ProductSerializer.filterFromJson(json);

            assertThat(f.getName()).isEqualTo("молоко");
            assertThat(f.getCategory()).isEqualTo("Молочне");
            assertThat(f.getMinPrice()).isNull();
            assertThat(f.getMaxPrice()).isNull();
            assertThat(f.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("filterFromJson парсить фільтр лише за ціною")
        void filterFromJsonPriceOnly() {
            String json = "{\"minPrice\":3.0}";
            ProductFilter f = ProductSerializer.filterFromJson(json);

            assertThat(f.getMinPrice()).isEqualTo(3.0);
            assertThat(f.getName()).isNull();
            assertThat(f.getCategory()).isNull();
        }

        @Test
        @DisplayName("pageResultToJson містить items і метадані пагінації")
        void pageResultToJson() {
            service.create("Товар А", "кат", 10, 5.0);
            service.create("Товар Б", "кат", 20, 10.0);
            ProductFilter f = ProductFilter.builder().pageSize(2).build();
            PageResult<Product> result = service.search(f);
            String json = ProductSerializer.pageResultToJson(result);

            assertThat(json).contains("\"totalItems\":2");
            assertThat(json).contains("\"items\":");
            assertThat(json).containsAnyOf("Товар А", "Товар Б");
        }

        @Test
        @DisplayName("серіалізація з лапками в назві — екранування")
        void escapeQuotesInName() {
            Product p = new Product("id1", "Сир \"Гауда\"", "Молочне", 10, 50.0);
            String json = ProductSerializer.productToJson(p);
            Product restored = ProductSerializer.productFromJson(json);
            assertThat(restored.getName()).isEqualTo("Сир \"Гауда\"");
        }
    }

    @Nested
    @DisplayName("StoreProcessor інтеграція")
    class StoreProcessorIntegrationTests {

        private StoreProcessor processor;

        @BeforeEach
        void setUp() {
            Store store = new Store();
            ProductService ps = new ProductService();
            processor = new StoreProcessor(store, ps);
        }

        private Packet makePacket(int type, String payload) {
            Message msg = new Message(type, 1, payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new Packet((byte) 1, 1L, msg);
        }

        private String response(Packet p) {
            return new String(p.getMessage().getData(), java.nio.charset.StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("type=10 CREATE через протокол")
        void createViaProtocol() {
            String json = "{\"name\":\"Масло\",\"category\":\"Молочне\",\"quantity\":50,\"price\":95.0}";
            String resp = response(processor.process(makePacket(10, json)));

            assertThat(resp).contains("\"name\":\"Масло\"");
            assertThat(resp).contains("\"id\":\"");
        }

        @Test
        @DisplayName("type=11 READ через протокол — знайдено")
        void readViaProtocol() {
            String createJson = "{\"name\":\"Йогурт\",\"category\":\"Молочне\",\"quantity\":30,\"price\":25.0}";
            String created = response(processor.process(makePacket(10, createJson)));
            String id = ProductSerializer.productFromJson(created).getId();

            String readResp = response(processor.process(makePacket(11, id)));
            assertThat(readResp).contains("\"name\":\"Йогурт\"");
        }

        @Test
        @DisplayName("type=11 READ через протокол — не знайдено")
        void readNotFound() {
            String resp = response(processor.process(makePacket(11, "bad-id")));
            assertThat(resp).isEqualTo("ERROR:NOT_FOUND");
        }

        @Test
        @DisplayName("type=13 DELETE через протокол")
        void deleteViaProtocol() {
            String createJson = "{\"name\":\"Сметана\",\"category\":\"Молочне\",\"quantity\":20,\"price\":40.0}";
            String created = response(processor.process(makePacket(10, createJson)));
            String id = ProductSerializer.productFromJson(created).getId();

            String deleteResp = response(processor.process(makePacket(13, id)));
            assertThat(deleteResp).isEqualTo("OK");

            String readResp = response(processor.process(makePacket(11, id)));
            assertThat(readResp).isEqualTo("ERROR:NOT_FOUND");
        }

        @Test
        @DisplayName("type=14 SEARCH з фільтром за категорією через протокол")
        void searchViaProtocol() {
            processor.process(makePacket(10,
                    "{\"name\":\"Продукт А\",\"category\":\"Тест\",\"quantity\":10,\"price\":5.0}"));
            processor.process(makePacket(10,
                    "{\"name\":\"Продукт Б\",\"category\":\"Тест\",\"quantity\":20,\"price\":10.0}"));
            processor.process(makePacket(10,
                    "{\"name\":\"Продукт В\",\"category\":\"Інша\",\"quantity\":5,\"price\":3.0}"));

            String resp = response(processor.process(makePacket(14,
                    "{\"category\":\"Тест\",\"page\":0,\"pageSize\":10}")));

            assertThat(resp).contains("\"totalItems\":2");
            assertThat(resp).containsAnyOf("Продукт А", "Продукт Б");
        }

        @Test
        @DisplayName("type=14 SEARCH фільтр лише за мінімальною ціною > 3")
        void searchByMinPriceOnly() {
            processor.process(makePacket(10,
                    "{\"name\":\"Дешевий\",\"category\":\"кат\",\"quantity\":10,\"price\":2.0}"));
            processor.process(makePacket(10,
                    "{\"name\":\"Дорогий\",\"category\":\"кат\",\"quantity\":10,\"price\":5.0}"));

            String resp = response(processor.process(makePacket(14, "{\"minPrice\":3.0}")));
            assertThat(resp).contains("\"totalItems\":1");
            assertThat(resp).contains("Дорогий");
        }

        @Test
        @DisplayName("старі команди Store (type=1,2,3) досі працюють після інтеграції")
        void oldCommandsStillWork() {
            Packet add = makePacket(3, "гречка,100");
            assertThat(response(processor.process(add))).isEqualTo("OK");

            Packet get = makePacket(1, "гречка");
            assertThat(response(processor.process(get))).isEqualTo("QUANTITY:100");
        }
        @Test
        @DisplayName("type=12 UPDATE через протокол — часткове оновлення та нульові значення")
        void updateViaProtocol() {
            String createJson = "{\"name\":\"Кефір\",\"category\":\"Молочне\",\"quantity\":50,\"price\":40.0}";
            String created = response(processor.process(makePacket(10, createJson)));
            String id = ProductSerializer.productFromJson(created).getId();

            String updateJson = "{\"id\":\"" + id + "\",\"quantity\":0,\"price\":45.5}";
            String updatedResp = response(processor.process(makePacket(12, updateJson)));

            org.assertj.core.api.Assertions.assertThat(updatedResp).contains("\"id\":\"" + id + "\"");
            org.assertj.core.api.Assertions.assertThat(updatedResp).contains("\"name\":\"Кефір\"");
            org.assertj.core.api.Assertions.assertThat(updatedResp).contains("\"category\":\"Молочне\"");
            org.assertj.core.api.Assertions.assertThat(updatedResp).contains("\"quantity\":0");
            org.assertj.core.api.Assertions.assertThat(updatedResp).contains("\"price\":45.5");
        }
    }
}
