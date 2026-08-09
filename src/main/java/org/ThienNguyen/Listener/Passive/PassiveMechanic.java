package org.ThienNguyen.Listener.Passive;

/**
 * Hợp đồng cho mọi mechanic (hành động) mà 1 passive thực thi.
 *
 * THAY ĐỔI SO VỚI BẢN CŨ: execute() giờ trả về boolean thay vì void.
 *   true  = mechanic thực thi thành công (ví dụ DAMAGE gây được damage, HEAL hồi được máu)
 *   false = thất bại / không áp dụng được (target null, target đã chết, không đủ điều kiện...)
 *
 * Giá trị trả về quyết định CÓ chạy "children" (mechanic con, khai báo trong yml) hay không.
 * Xem AbstractMechanic — mechanic mới nên kế thừa AbstractMechanic và implement doExecute(),
 * không cần tự lo phần target-resolve hay children, AbstractMechanic đã xử lý sẵn.
 */
public interface PassiveMechanic {
    boolean execute(PassiveContext ctx);
}